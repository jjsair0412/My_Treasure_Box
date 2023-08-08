package com.presignedurl.awss3presignedurl.logic.openstackSwift;

import com.amazonaws.HttpMethod;

import java.net.URL;

public interface swiftService {
    URL simpleUpload(String bucket, String filePath, HttpMethod httpMethod);
}
