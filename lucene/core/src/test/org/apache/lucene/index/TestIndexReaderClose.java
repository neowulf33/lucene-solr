package org.apache.lucene.index;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.TestUtil;

/**
 */
public class TestIndexReaderClose extends LuceneTestCase {

  public void testCloseUnderException() throws IOException {
    final int iters = 1000 +  1 + random().nextInt(20);
    for (int j = 0; j < iters; j++) {
      Directory dir = newDirectory();
      IndexWriter writer = new IndexWriter(dir, newIndexWriterConfig(random(), new MockAnalyzer(random())));
      writer.commit();
      writer.close();
      DirectoryReader open = DirectoryReader.open(dir);
      final boolean throwOnClose = !rarely();
      AtomicReader wrap = SlowCompositeReaderWrapper.wrap(open);
      FilterAtomicReader reader = new FilterAtomicReader(wrap) {
        @Override
        protected void doClose() throws IOException {
          super.doClose();
          if (throwOnClose) {
           throw new IllegalStateException("BOOM!");
          }
        }
      };
      int listenerCount = random().nextInt(20);
      AtomicInteger count = new AtomicInteger();
      boolean faultySet = false;
      for (int i = 0; i < listenerCount; i++) {
          if (rarely()) {
            faultySet = true;
            reader.addReaderClosedListener(new FaultyListener());
          } else {
            count.incrementAndGet();
            reader.addReaderClosedListener(new CountListener(count));
          }
      }
      if (!faultySet && !throwOnClose) {
        reader.addReaderClosedListener(new FaultyListener());
      }
      try {
        reader.close();
        fail("expected Exception");
      } catch (IllegalStateException ex) {
        if (throwOnClose) {
          assertEquals("BOOM!", ex.getMessage());
        } else {
          assertEquals("GRRRRRRRRRRRR!", ex.getMessage());
        }
      }

      try {
        reader.fields();
        fail("we are closed");
      } catch (AlreadyClosedException ex) {
      }

      if (random().nextBoolean()) {
        reader.close(); // call it again
      }
      assertEquals(0, count.get());
      wrap.close();
      dir.close();
    }
  }

  public void testCoreListenerOnWrapper() throws IOException {
    RandomIndexWriter w = new RandomIndexWriter(random(), newDirectory());
    final int numDocs = TestUtil.nextInt(random(), 1, 5);
    for (int i = 0; i < numDocs; ++i) {
      w.addDocument(new Document());
      if (random().nextBoolean()) {
        w.commit();
      }
    }
    w.commit();
    w.close();

    final IndexReader reader = DirectoryReader.open(w.w.getDirectory());
    final AtomicReader atomicReader = SlowCompositeReaderWrapper.wrap(reader);
    
    final int numListeners = TestUtil.nextInt(random(), 1, 10);
    final List<AtomicReader.CoreClosedListener> listeners = new ArrayList<>();
    AtomicInteger counter = new AtomicInteger(numListeners);
    
    for (int i = 0; i < numListeners; ++i) {
      CountCoreListener listener = new CountCoreListener(counter);
      listeners.add(listener);
      atomicReader.addCoreClosedListener(listener);
    }
    for (int i = 0; i < 100; ++i) {
      atomicReader.addCoreClosedListener(listeners.get(random().nextInt(listeners.size())));
    }
    final int removed = random().nextInt(numListeners);
    Collections.shuffle(listeners);
    for (int i = 0; i < removed; ++i) {
      atomicReader.removeCoreClosedListener(listeners.get(i));
    }
    assertEquals(numListeners, counter.get());
    // make sure listeners are registered on the wrapped reader and that closing any of them has the same effect
    if (random().nextBoolean()) {
      reader.close();
    } else {
      atomicReader.close();
    }
    assertEquals(removed, counter.get());
    w.w.getDirectory().close();
  }

  private static final class CountCoreListener implements AtomicReader.CoreClosedListener {

    private final AtomicInteger count;

    public CountCoreListener(AtomicInteger count) {
      this.count = count;
    }

    @Override
    public void onClose(Object coreCacheKey) {
      count.decrementAndGet();
    }

  }

  private static final class CountListener implements IndexReader.ReaderClosedListener  {
    private final AtomicInteger count;

    public CountListener(AtomicInteger count) {
      this.count = count;
    }

    @Override
    public void onClose(IndexReader reader) {
      count.decrementAndGet();
    }
  }

  private static final class FaultyListener implements IndexReader.ReaderClosedListener {

    @Override
    public void onClose(IndexReader reader) {
      throw new IllegalStateException("GRRRRRRRRRRRR!");
    }
  }

}
