package com.presignedurl.awss3presignedurl.logic.openstackSwift;

import com.amazonaws.HttpMethod;
import com.presignedurl.awss3presignedurl.logic.openstackSwift.config.openstackConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URL;

@Service
@RequiredArgsConstructor
@Slf4j
public class swiftServiceImpl implements swiftService{

    private final openstackConfig openstackConfig;

    @Override
    public URL simpleUpload(String bucket, String filePath, HttpMethod httpMethod) {
        return null;
    }
}
