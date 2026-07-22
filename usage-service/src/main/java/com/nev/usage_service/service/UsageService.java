package com.nev.usage_service.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.nev.kafka.event.AlertingEvent;
import com.nev.kafka.event.EnergyUsageEvent;

import com.nev.usage_service.client.DeviceClient;
import com.nev.usage_service.client.UserClient;
import com.nev.usage_service.dto.DeviceDto;
import com.nev.usage_service.dto.UserDto;
import com.nev.usage_service.model.DeviceEnergy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UsageService {

    private InfluxDBClient influxDBClient;
    private DeviceClient deviceClient;
    private UserClient userClient;
    private KafkaTemplate<String ,AlertingEvent> kafkaTemplate;


    @Value("${influx.bucket}")
    private String influxBucket;

    @Value("${influx.org}")
    private String influxOrg;

    public UsageService(InfluxDBClient influxDBClient,
                        DeviceClient deviceClient,
                        UserClient userClient,
                        KafkaTemplate<String,AlertingEvent> kafkaTemplate){
        this.influxDBClient = influxDBClient;
        this.deviceClient = deviceClient;
        this.userClient = userClient;
        this.kafkaTemplate = kafkaTemplate;
    }


    @KafkaListener(topics = "energy-usage",groupId = "usage-service")
    public void energyUsageEvent(EnergyUsageEvent energyUsageEvent){
        log.info("Received energy used event: {}",energyUsageEvent);
        Point point = Point.measurement("energy-usage")
                .addTag("deviceId",String.valueOf(energyUsageEvent.deviceId()))
                .addField("energyConsumed",energyUsageEvent.energyConsumed())
                .time(energyUsageEvent.timeStamp(), WritePrecision.MS);
        influxDBClient.getWriteApiBlocking().writePoint(influxBucket,influxOrg,point);
    }

    @Scheduled(cron = "*/10 * * * * *")
    public void aggregatedDeviceEnergyUsage(){
        final Instant now = Instant.now();
        final Instant oneHourAgo = now.minusSeconds(3600);

        String fluxQuery = String.format("""
        from(bucket: "%s")
          |> range(start: time(v : "%s"), stop: time(v: "%s"))
          |> filter(fn: (r) => r["_measurement"] == "energy-usage")
          |> filter(fn: (r) => r["_field"] == "energyConsumed")
          |> group(columns: ["deviceId"])
          |> sum(column: "_value")
        """,influxBucket, oneHourAgo.toString(),now);
        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(fluxQuery,influxOrg);

        List<DeviceEnergy> deviceEnergies= new ArrayList<>();

        for(FluxTable table: tables){
            for(FluxRecord record: table.getRecords()){
                String deviceIdStr = (String) record.getValueByKey("deviceId");
                Double energyConsumed = record.getValueByKey("_value") instanceof Number ?
                        ((Number) record.getValueByKey("_value")).doubleValue() :0.0;

                deviceEnergies.add(
                        DeviceEnergy.builder()
                                .deviceId(Long.valueOf(deviceIdStr))
                                .energyConsumed(energyConsumed)
                                .build()
                );
            }
        }
        log.info("aggregated device energy for past hour: {}",deviceEnergies);

        for(DeviceEnergy deviceEnergy: deviceEnergies){
            final DeviceDto deviceResponse = deviceClient.getDeviceById(deviceEnergy.getDeviceId());
            if(deviceResponse == null || deviceResponse.id() == null){
                log.warn("Device not found with id: {}",deviceEnergy.getDeviceId());
                continue;
            }
            deviceEnergy.setUserId(deviceEnergy.getUserId());
        }
        deviceEnergies.removeIf(de -> de.getUserId() == null);
        Map<Long,List<DeviceEnergy>> userDeviceEnergyMap=
                deviceEnergies.stream()
                        .collect(Collectors.groupingBy(DeviceEnergy::getUserId));
        log.info("user-device energy map: {}",userDeviceEnergyMap);

        List<Long> userIds = new ArrayList<>(userDeviceEnergyMap.keySet());
        final Map<Long,Double> userThresholdMap = new HashMap<>();
        final Map<Long,String > userEmailMap = new HashMap<>();

        for(final Long userId: userIds){
            try{
                UserDto user = userClient.getUserById(userId);
                if(user == null || user.id() == null || !user.alerting()){
                    log.warn("user not found or alerting is disabled for ID: {}",userId);
                }
                userThresholdMap.put(userId,user.energyAlertingThreshold());
                userEmailMap.put(userId,user.email());
            }
            catch (Exception e){
                log.error("failed to fetch details for user with id: {}",userId);
            }
        }
        log.info("user threshold map: {}",userThresholdMap);

        final List<Long> alertedUsers = new ArrayList<>(userThresholdMap.keySet());

        for(final Long userId: alertedUsers){
            final Double threshold = userThresholdMap.get(userId);
            final List<DeviceEnergy> devices = userDeviceEnergyMap.get(userId);

            final Double totalConsumption = devices.stream()
                    .mapToDouble(DeviceEnergy::getEnergyConsumed)
                    .sum();
            if(totalConsumption > threshold){
                log.info("Alert: User id {} has exceeded the threshold!"+
                        "total consumption:{},threshold: {}",
                        userId,totalConsumption,threshold);
                final AlertingEvent alertingEvent =AlertingEvent.builder()
                        .userId(userId)
                        .message("Energy Consumption has been exceeded!")
                        .threshold(threshold)
                        .energyConsumed(totalConsumption)
                        .email(userEmailMap.get(userId))
                        .build();
                kafkaTemplate.send("energy-alerts",alertingEvent);
            }
        }
    }

}
