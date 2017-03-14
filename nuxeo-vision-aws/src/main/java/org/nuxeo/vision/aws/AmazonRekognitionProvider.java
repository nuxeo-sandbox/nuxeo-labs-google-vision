/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Remi Cattiau
 *     Michael Vachette
 */
package org.nuxeo.vision.aws;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.google.common.collect.ImmutableList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.vision.core.service.VisionFeature;
import org.nuxeo.vision.core.service.VisionProvider;
import org.nuxeo.vision.core.service.VisionResponse;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AmazonRekognitionProvider implements VisionProvider {

    public static final String REGION_PARAM = "region";

    public static final String ACCESS_KEY_PARAM = "accessKey";

    public static final String SECRET_KEY_PARAM = "secretKey";

    protected static final long _5MB = 5242880;

    protected static final List<String> SUPPORTED_FORMAT = ImmutableList.of("image/jpeg", "image/png");

    private volatile AmazonRekognition client = null;

    private Map<String, String> parameters;

    public AmazonRekognitionProvider(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public List<VisionResponse> execute(List<Blob> blobs, List<VisionFeature> features, int maxResults)
            throws IOException, GeneralSecurityException, IllegalStateException {
        ArrayList<VisionResponse> result = new ArrayList<>();
        for (Blob blob : blobs) {
            result.add(new AmazonRekognitionResponse(getClient().detectLabels(
                    new DetectLabelsRequest().withImage(new com.amazonaws.services.rekognition.model.Image().withBytes(
                            ByteBuffer.wrap(blob.getByteArray()))).withMaxLabels(maxResults))));
        }
        return result;
    }

    @Override
    public List<VisionFeature> getSupportedFeatures() {
        return ImmutableList.of(VisionFeature.LABEL_DETECTION, VisionFeature.FACE_DETECTION);
    }

    @Override
    public boolean checkBlobs(List<Blob> blobs) throws IOException {
        for (Blob blob : blobs) {
            long size = blob.getLength();
            if (size <= 0) {
                throw new IOException("Could not read the blob size");
            }
            if (size > _5MB) {
                return false;
            }
            if (!SUPPORTED_FORMAT.contains(blob.getMimeType())) {
                return false;
            }

        }
        return true;
    }

    @Override
    public Object getNativeClient() {
        return getClient();
    }

    private AmazonRekognition getClient() {
        // thread safe lazy initialization of the AWS Rekognition client
        // see https://en.wikipedia.org/wiki/Double-checked_locking
        AmazonRekognition result = client;
        if (result == null) {
            synchronized (this) {
                result = client;
                if (result == null) {
                    AmazonRekognitionClientBuilder builder = AmazonRekognitionClientBuilder.standard();
                    builder.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                            parameters.get(ACCESS_KEY_PARAM), parameters.get(SECRET_KEY_PARAM))));
                    builder.withRegion(parameters.get(REGION_PARAM));
                    result = client = builder.build();
                }
            }
        }
        return result;
    }

}
