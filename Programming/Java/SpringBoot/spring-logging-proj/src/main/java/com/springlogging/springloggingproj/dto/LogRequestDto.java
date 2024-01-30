package com.springlogging.springloggingproj.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LogRequestDto {
    private String httpMethod;
    private String uri;
    private long logTime;
//    private String server_ip;
    private long elapsed_time;
    private String trace_id;


    public LogRequestDto of (
            String httpMethod,
            String uri,
            long logTime,
//            String server_ip,
            long elapsed_time,
            String trace_id
    ) {
        return LogRequestDto.builder()
                .httpMethod(httpMethod)
                .uri(uri)
                .logTime(logTime)
//                .server_ip(server_ip)
                .elapsed_time(elapsed_time)
                .trace_id(trace_id)
                .build();
    }

}
