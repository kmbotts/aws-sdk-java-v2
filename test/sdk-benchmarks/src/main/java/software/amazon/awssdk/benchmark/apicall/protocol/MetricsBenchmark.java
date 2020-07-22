/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.benchmark.apicall.protocol;

import static software.amazon.awssdk.benchmark.utils.BenchmarkConstant.ERROR_JSON_BODY;
import static software.amazon.awssdk.benchmark.utils.BenchmarkConstant.JSON_ALL_TYPES_REQUEST;
import static software.amazon.awssdk.benchmark.utils.BenchmarkConstant.JSON_BODY;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.benchmark.utils.MockH2Server;
import software.amazon.awssdk.benchmark.utils.MockHttpClient;
import software.amazon.awssdk.benchmark.utils.MockServer;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;

/**
 * Benchmarking for running with different protocols.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 15, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(1) // To reduce difference between each run
@BenchmarkMode(Mode.Throughput)
public class MetricsBenchmark {
    private MockServer mockServer;
    private MockH2Server mockH2Server;
    private ProtocolRestJsonClient syncClient;
    private ProtocolRestJsonAsyncClient asyncClient;
    private ProtocolRestJsonAsyncClient asyncH2Client;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        mockServer = new MockServer();
        mockServer.start();
        mockH2Server = new MockH2Server(false);
        mockH2Server.start();
        syncClient = ProtocolRestJsonClient.builder()
                                           .endpointOverride(mockServer.getHttpUri())
                                           .httpClientBuilder(ApacheHttpClient.builder())
                                           .build();
        asyncClient = ProtocolRestJsonAsyncClient.builder()
                                                 .endpointOverride(mockServer.getHttpUri())
                                                 .httpClientBuilder(NettyNioAsyncHttpClient.builder())
                                                 .build();
        asyncH2Client = ProtocolRestJsonAsyncClient.builder()
                                                   .endpointOverride(mockH2Server.getHttpUri())
                                                   .httpClientBuilder(NettyNioAsyncHttpClient.builder())
                                                   .build();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        mockServer.stop();
        mockH2Server.stop();
        syncClient.close();
        asyncClient.close();
        asyncH2Client.close();
    }

    @Benchmark
    public void benchmarkSyncH1Client() {
        syncClient.allTypes();
    }

//    @Threads(25)
//    @Benchmark
//    public void benchmarkAsyncH1Client() {
//        asyncClient.allTypes().join();
//    }
//
//    @Threads(25)
//    @Benchmark
//    public void benchmarkAsyncH2Client() {
//        asyncH2Client.allTypes().join();
//    }

    public static void main(String... args) throws Exception {
        Options opt = new OptionsBuilder()
            .include(MetricsBenchmark.class.getSimpleName())
            .build();
        new Runner(opt).run();
    }
}
