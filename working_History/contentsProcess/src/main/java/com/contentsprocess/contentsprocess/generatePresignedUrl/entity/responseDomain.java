package com.contentsprocess.contentsprocess.generatePresignedUrl.entity;

import com.contentsprocess.contentsprocess.common.statusEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.net.URL;

@Getter
@Setter
@RequiredArgsConstructor
public class responseDomain {
    private statusEnum status;
    private URL url;

    @Builder
    public responseDomain(statusEnum status, URL url) {
        this.status = status;
        this.url = url;
    }
}
