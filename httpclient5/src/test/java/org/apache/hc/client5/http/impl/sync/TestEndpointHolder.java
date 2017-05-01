/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.apache.hc.client5.http.impl.sync;

import org.apache.hc.client5.http.io.ConnectionEndpoint;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.io.ShutdownType;
import org.apache.hc.core5.util.TimeValue;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@SuppressWarnings({"static-access"}) // test code
public class TestEndpointHolder {

    @Mock
    private Logger log;
    @Mock
    private HttpClientConnectionManager mgr;
    @Mock
    private ConnectionEndpoint endpoint;

    private EndpointHolder connHolder;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        connHolder = new EndpointHolder(log, mgr, endpoint);
    }

    @Test
    public void testAbortConnection() throws Exception {
        connHolder.abortConnection();

        Assert.assertTrue(connHolder.isReleased());

        Mockito.verify(endpoint).shutdown(ShutdownType.IMMEDIATE);
        Mockito.verify(mgr).release(endpoint, null, TimeValue.ZERO_MILLISECONDS);

        connHolder.abortConnection();

        Mockito.verify(endpoint, Mockito.times(1)).shutdown(ShutdownType.IMMEDIATE);
        Mockito.verify(mgr, Mockito.times(1)).release(
                Mockito.<ConnectionEndpoint>any(), Mockito.anyObject(), Mockito.<TimeValue>any());
    }

    @Test
    public void testCancell() throws Exception {
        Assert.assertTrue(connHolder.cancel());

        Assert.assertTrue(connHolder.isReleased());

        Mockito.verify(endpoint).shutdown(ShutdownType.IMMEDIATE);
        Mockito.verify(mgr).release(endpoint, null, TimeValue.ZERO_MILLISECONDS);

        Assert.assertFalse(connHolder.cancel());

        Mockito.verify(endpoint, Mockito.times(1)).shutdown(ShutdownType.IMMEDIATE);
        Mockito.verify(mgr, Mockito.times(1)).release(
                Mockito.<ConnectionEndpoint>any(), Mockito.anyObject(), Mockito.<TimeValue>any());
    }

    @Test
    public void testReleaseConnectionReusable() throws Exception {
        connHolder.setState("some state");
        connHolder.setValidFor(TimeValue.ofSeconds(100));
        connHolder.markReusable();

        connHolder.releaseConnection();

        Assert.assertTrue(connHolder.isReleased());

        Mockito.verify(endpoint, Mockito.never()).close();
        Mockito.verify(mgr).release(endpoint, "some state", TimeValue.ofSeconds(100));

        connHolder.releaseConnection();

        Mockito.verify(mgr, Mockito.times(1)).release(
                Mockito.<ConnectionEndpoint>any(), Mockito.anyObject(), Mockito.<TimeValue>any());
    }

    @Test
    public void testReleaseConnectionNonReusable() throws Exception {
        connHolder.setState("some state");
        connHolder.setValidFor(TimeValue.ofSeconds(100));
        connHolder.markNonReusable();

        connHolder.releaseConnection();

        Assert.assertTrue(connHolder.isReleased());

        Mockito.verify(endpoint, Mockito.times(1)).close();
        Mockito.verify(mgr).release(endpoint, null, TimeValue.ZERO_MILLISECONDS);

        connHolder.releaseConnection();

        Mockito.verify(mgr, Mockito.times(1)).release(
                Mockito.<ConnectionEndpoint>any(), Mockito.anyObject(), Mockito.<TimeValue>any());
    }

}
