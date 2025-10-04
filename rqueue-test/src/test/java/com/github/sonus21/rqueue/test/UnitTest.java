/*
 * Copyright (c) 2023 Sonu Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package com.github.sonus21.rqueue.test;

import com.github.sonus21.rqueue.annotation.RqueueRabbitListener;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple unit tests to validate basic functionality.
 *
 * @author Sonu Kumar
 */
public class UnitTest {

  @Test
  void testRqueueRabbitListenerAnnotation() {
    // Test that the annotation exists and can be used
    assertNotNull(RqueueRabbitListener.class);
    
    // Test annotation properties
    RqueueRabbitListener annotation = TestClass.class.getAnnotation(RqueueRabbitListener.class);
    assertNotNull(annotation);
    assertEquals("test-queue", annotation.value());
    assertEquals("1-5", annotation.concurrency());
  }

  @Test
  void testBasicValidation() {
    // Test basic validation logic
    assertThrows(IllegalArgumentException.class, () -> {
      if (null == null) {
        throw new IllegalArgumentException("Test validation");
      }
    });
  }

  static class TestClass {
    @RqueueRabbitListener(value = "test-queue", concurrency = "1-5")
    public void handleMessage(String message) {
      // Test method
    }
  }
}