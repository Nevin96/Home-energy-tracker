package com.nev.usage_service.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.nev.kafka.event.EnergyUsageEvent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class UsageService {

    private InfluxDBClient influxDBClient;

    @Value("${influx.bucket}")
    private String influxBucket;

    @Value("${influx.org}")
    private String influxOrg;

    public UsageService(InfluxDBClient influxDBClient){
        this.influxDBClient = influxDBClient;
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
    }
}
