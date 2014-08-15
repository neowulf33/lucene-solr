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

package org.apache.solr.response;

import org.apache.solr.SolrTestCaseJ4;
import org.junit.*;
import org.apache.lucene.util.LuceneTestCase.SuppressCodecs;

@SuppressCodecs({"Lucene3x", "Lucene40","Lucene41","Lucene42","Lucene45"})
public class TestSortingResponseWriter extends SolrTestCaseJ4 {
  @BeforeClass
  public static void beforeClass() throws Exception {
    initCore("solrconfig-sortingresponse.xml","schema-sortingresponse.xml");
    createIndex();
  }

  public static void createIndex() {
    assertU(adoc("id","1",
                 "floatdv","2.1",
                 "intdv", "1",
                 "stringdv", "hello world",
                 "longdv", "323223232323",
                 "doubledv","2344.345",
                 "intdv_m","100",
                 "intdv_m","250",
                 "floatdv_m", "123.321",
                 "floatdv_m", "345.123",
                 "doubledv_m", "3444.222",
                 "doubledv_m", "23232.2",
                 "longdv_m", "43434343434",
                 "longdv_m", "343332",
                 "stringdv_m", "manchester city",
                 "stringdv_m", "liverpool",
                 "stringdv_m", "Everton"));

    assertU(commit());
    assertU(adoc("id","2", "floatdv","2.1", "intdv", "2", "stringdv", "hello world", "longdv", "323223232323","doubledv","2344.344"));
    assertU(commit());
    assertU(adoc("id","3",
        "floatdv","2.1",
        "intdv", "3",
        "stringdv", "chello world",
        "longdv", "323223232323",
        "doubledv","2344.346",
        "intdv_m","100",
        "intdv_m","250",
        "floatdv_m", "123.321",
        "floatdv_m", "345.123",
        "doubledv_m", "3444.222",
        "doubledv_m", "23232.2",
        "longdv_m", "43434343434",
        "longdv_m", "343332",
        "stringdv_m", "manchester city",
        "stringdv_m", "liverpool",
        "stringdv_m", "everton"));
    assertU(commit());


  }

  @Test
  public void testSortingOutput() throws Exception {

    //Test single value DocValue output
    String s =  h.query(req("q", "id:1", "qt", "/export", "fl", "floatdv,intdv,stringdv,longdv,doubledv", "sort", "intdv asc"));
    assertEquals(s, "{\"numFound\":1, \"docs\":[{\"floatdv\":2.1,\"intdv\":1,\"stringdv\":\"hello world\",\"longdv\":323223232323,\"doubledv\":2344.345}]}");

    //Test multiValue docValues output
    s =  h.query(req("q", "id:1", "qt", "/export", "fl", "intdv_m,floatdv_m,doubledv_m,longdv_m,stringdv_m", "sort", "intdv asc"));
    System.out.println(s);
    assertEquals(s, "{\"numFound\":1, \"docs\":[{\"intdv_m\":[100,250],\"floatdv_m\":[123.321,345.123],\"doubledv_m\":[3444.222,23232.2],\"longdv_m\":[343332,43434343434],\"stringdv_m\":[\"Everton\",\"liverpool\",\"manchester city\"]}]}");
    //Test single sort param is working
    s =  h.query(req("q", "id:(1 2)", "qt", "/export", "fl", "intdv", "sort", "intdv desc"));
    System.out.println("Output:"+s);
    assertEquals(s, "{\"numFound\":2, \"docs\":[{\"intdv\":2},{\"intdv\":1}]}");

    s =  h.query(req("q", "id:(1 2)", "qt", "/export", "fl", "intdv", "sort", "intdv asc"));
    assertEquals(s, "{\"numFound\":2, \"docs\":[{\"intdv\":1},{\"intdv\":2}]}");

    //Test multi-sort params
    s =  h.query(req("q", "id:(1 2)", "qt", "/export", "fl", "intdv", "sort", "floatdv asc,intdv desc"));
    assertEquals(s, "{\"numFound\":2, \"docs\":[{\"intdv\":2},{\"intdv\":1}]}");

    s =  h.query(req("q", "id:(1 2)", "qt", "/export", "fl", "intdv", "sort", "floatdv desc,intdv asc"));
    assertEquals(s, "{\"numFound\":2, \"docs\":[{\"intdv\":1},{\"intdv\":2}]}");

    //Test three sort fields
    s =  h.query(req("q", "id:(1 2 3)", "qt", "/export", "fl", "intdv", "sort", "floatdv asc,stringdv asc,intdv desc"));
    assertEquals(s, "{\"numFound\":3, \"docs\":[{\"intdv\":3},{\"intdv\":2},{\"intdv\":1}]}");

    //Test three sort fields
    s =  h.query(req("q", "id:(1 2 3)", "qt", "/export", "fl", "intdv", "sort", "floatdv asc,stringdv desc,intdv asc"));
    assertEquals(s, "{\"numFound\":3, \"docs\":[{\"intdv\":1},{\"intdv\":2},{\"intdv\":3}]}");

    //Test four sort fields
    s =  h.query(req("q", "id:(1 2 3)", "qt", "/export", "fl", "intdv", "sort", "floatdv asc,floatdv desc,floatdv asc,intdv desc"));
    assertEquals(s, "{\"numFound\":3, \"docs\":[{\"intdv\":3},{\"intdv\":2},{\"intdv\":1}]}");

    s =  h.query(req("q", "id:(1 2 3)", "qt", "/export", "fl", "intdv", "sort", "doubledv desc"));
    System.out.println("Results:"+s);
    assertEquals(s, "{\"numFound\":3, \"docs\":[{\"intdv\":3},{\"intdv\":1},{\"intdv\":2}]}");

  }
}