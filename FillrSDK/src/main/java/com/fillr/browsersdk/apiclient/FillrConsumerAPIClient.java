/*
 * Copyright 2015-present Pop Tech Pty Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fillr.browsersdk.apiclient;

import com.squareup.okhttp.OkHttpClient;

import retrofit.RestAdapter;
import retrofit.client.OkClient;

public class FillrConsumerAPIClient {


    public static final String API_BASE_URL = "https://d2o8n2jotd2j7i.cloudfront.net/widget/android/sdk";

    private static RestAdapter.Builder builder = new RestAdapter.Builder()
                                                    .setEndpoint(API_BASE_URL)
                                                    .setClient(new OkClient(new OkHttpClient()));

    public static <S> S createFillrAPIService(Class<S> serviceClass) {
        RestAdapter adapter = builder.build();
        return adapter.create(serviceClass);
    }

}
