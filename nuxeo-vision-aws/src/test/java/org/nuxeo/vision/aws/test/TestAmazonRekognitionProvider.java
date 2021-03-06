/*
 * (C) Copyright 2015-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Michael Vachette
 */
package org.nuxeo.vision.aws.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.vision.aws.AmazonRekognitionProvider;
import org.nuxeo.vision.core.image.TextEntity;
import org.nuxeo.vision.core.service.Vision;
import org.nuxeo.vision.core.service.VisionFeature;
import org.nuxeo.vision.core.service.VisionResponse;

import com.amazonaws.services.rekognition.model.DetectModerationLabelsResult;

@RunWith(FeaturesRunner.class)
@org.nuxeo.runtime.test.runner.Features({ PlatformFeature.class })
@Deploy({ "nuxeo-vision-core", "nuxeo-vision-aws" })
public class TestAmazonRekognitionProvider {

    public static final String PROVIDES_AWS_CREDENTIALS = "provides.aws.credentials";

    @Inject
    Vision visionService;

    protected AmazonRekognitionProvider provider = null;

    @Before
    public void testProviderisLoaded() {
        assertNotNull(visionService.getProvider("aws"));
    }

    @Test
    public void testLabelFeature() throws IOException, GeneralSecurityException {

        Assume.assumeTrue("Credentials not set", areCredentialsSet());

        File file = new File(getClass().getResource("/files/plane.jpg").getPath());
        Blob blob = new FileBlob(file);
        List<VisionResponse> results = getProvider().execute(Arrays.asList(blob),
                Arrays.asList(VisionFeature.LABEL_DETECTION.toString()), 5);
        assertEquals(1, results.size());
        List<TextEntity> labels = results.get(0).getClassificationLabels();
        assertNotNull(labels);
        assertTrue(labels.size() > 0);
        System.out.print(labels);
    }

    @Test
    public void testMultipleBlobs() throws IOException, GeneralSecurityException {

        Assume.assumeTrue("Credentials not set", areCredentialsSet());

        List<Blob> blobs = new ArrayList<>();
        blobs.add(new FileBlob(new File(getClass().getResource("/files/plane.jpg").getPath())));
        blobs.add(new FileBlob(new File(getClass().getResource("/files/text.png").getPath())));

        List<VisionResponse> results = getProvider().execute(blobs,
                Arrays.asList(VisionFeature.LABEL_DETECTION.toString()), 5);
        assertTrue(results.size() == 2);
    }

    @Test
    public void testSafeContent() throws IOException, GeneralSecurityException {

        Assume.assumeTrue("Credentials not set", areCredentialsSet());

        List<Blob> blobs = new ArrayList<>();
        blobs.add(new FileBlob(new File(getClass().getResource("/files/text.png").getPath())));

        List<VisionResponse> results = getProvider().execute(blobs,
                Arrays.asList(VisionFeature.SAFE_SEARCH_DETECTION.toString()), 5);
        assertTrue(results.size() == 2);
        DetectModerationLabelsResult result = (DetectModerationLabelsResult) results.get(1).getNativeObject();
        assertEquals(0, result.getModerationLabels().size());
    }

    protected AmazonRekognitionProvider getProvider() {
        if (provider != null) {
            return provider;
        }
        provider = new AmazonRekognitionProvider(new HashMap<>());
        return provider;
    }

    protected boolean areCredentialsSet() {
        return Boolean.getBoolean(PROVIDES_AWS_CREDENTIALS);
    }
}
