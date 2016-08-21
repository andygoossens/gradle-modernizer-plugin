/*
 * Copyright 2014 the original author or authors.
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
 */
package com.github.andygoossens.gradle.plugins.utils

import java.nio.charset.Charset

class StreamUtils {
    
    private static final Charset UTF_8 = Charset.forName("UTF-8")

    static def closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
            // swallow exception
        }
    }

    static Collection<String> readAllLines(InputStream is) {
        Collection<String> lines = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, UTF_8));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            lines.add(line);
        }
        return lines;
    }
}
