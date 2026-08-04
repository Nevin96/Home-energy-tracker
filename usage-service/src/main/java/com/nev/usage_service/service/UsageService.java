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
import com.nev.usage_service.dto.UsageDto;
import com.nev.usage_service.dto.UserDto;
import com.nev.usage_service.model.Device;
import com.nev.usage_service.model.DeviceEnergy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
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
//        log.info("Received energy used event: {}",energyUsageEvent);
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
            deviceEnergy.setUserId(deviceResponse.userId());
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

    public UsageDto getXDaysUsageForUser(Long userId, int days) {
        log.info("Getting usage fot userId {} over past {} days",userId,days);
        final List<DeviceDto> devicesDto = deviceClient.getAllDevicesForUser(userId);

        final List<Device> devices = new ArrayList<>();
        for(DeviceDto deviceDto: devicesDto){
            devices.add(Device.builder()
                    .id(deviceDto.id())
                    .name(deviceDto.name())
                    .location(deviceDto.location())
                    .type(deviceDto.type())
                    .userId(deviceDto.userId())
                    .build());
        }

        if (devices == null || devices.isEmpty()){
            return UsageDto.builder()
                    .userId(userId)
                    .devices(Collections.emptyList())
                    .build();
        }
        List<String> deviceIdStrings = devices.stream()
                .map(Device::getId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList();

        final Instant now = Instant.now();
        final Instant start = now.minusSeconds((long) days * 24 *3600);

        final String deviceFilter = deviceIdStrings.stream()
                .map(idStr -> String.format("r[\"deviceId\"] == \"%s\"",idStr))
                .collect(Collectors.joining(" or "));

        String fluxQuery = String.format("""
         from(bucket: "%s")
             |> range(start: time(v: "%s"), stop: time(v: "%s"))
             |> filter(fn: (r) => r["_measurement"] == "energy-usage")
             |> filter(fn: (r) => r["_field"] == "energyConsumed")
             |> filter(fn: (r) => %s)
             |> group(columns: ["deviceId"])
             |> sum(column: "_value")
         """,influxBucket,start.toString(),now.toString(),deviceFilter);

        final  Map<Long,Double> aggregatedMap = new HashMap<>();
        log.info("Flux Query:\n{}", fluxQuery);
        try{
            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(fluxQuery,influxOrg);

            for(FluxTable table: tables){
                for(FluxRecord record: table.getRecords()){
                    Object deviceIdObj = record.getValueByKey("deviceId");
                    String deviceIdStr = deviceIdObj == null ? null : deviceIdObj.toString();
                    if(deviceIdStr == null) continue;

                    Double energyConsumed = record.getValueByKey("_value") instanceof Number
                            ? ((Number) record.getValueByKey("_value")).doubleValue()
                            :0.0;
                    try{
                        Long deviceId = Long.valueOf(deviceIdStr);
                        aggregatedMap.put(deviceId, aggregatedMap.getOrDefault(deviceId,0.0) + energyConsumed);
                    } catch(NumberFormatException nfe) {
                        log.warn("failed to parse deviceId from flux record: {}",deviceIdStr);
                    }
                }
            }
        } catch (Exception e){
            log.error("Failed to query influxDB for user {} usage over {} days: {}",userId,days,e.getMessage());
            devices.forEach(d -> d.setEnergyConsumed(0.0));
            return UsageDto.builder()
                    .userId(userId)
                    .devices(null)
                    .build();

        }
        for (Device device : devices){
            if(device == null || device.getId()== null){
                continue;
            }
            device.setEnergyConsumed(aggregatedMap.getOrDefault(device.getId(),0.0));
        }
        log.info("Aggregated energy consumption for userId {} : {}",userId,aggregatedMap);

        final List<DeviceDto> resultDevices = devices.stream()
                .map(d -> DeviceDto.builder()
                        .id(d.getId())
                        .name(d.getName())
                        .location(d.getLocation())
                        .type(d.getType())
                        .energyConsumed(d.getEnergyConsumed())
                        .userId(d.getUserId())
                        .build())
                .toList();
        return UsageDto.builder()
                .userId(userId)
                .devices(resultDevices)
                .build();
    }
}
