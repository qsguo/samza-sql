/*
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
 */

package org.apache.samza.sql.bench.slidingwindow;

import org.apache.avro.Schema;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelProtoDataType;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.samza.config.Config;
import org.apache.samza.sql.api.data.Relation;
import org.apache.samza.sql.api.data.Tuple;
import org.apache.samza.sql.data.IntermediateMessageTuple;
import org.apache.samza.sql.data.TupleConverter;
import org.apache.samza.sql.operators.SimpleOperatorImpl;
import org.apache.samza.sql.physical.insert.InsertToStreamSpec;
import org.apache.samza.sql.schema.AvroSchemaUtils;
import org.apache.samza.system.OutgoingMessageEnvelope;
import org.apache.samza.system.SystemStream;
import org.apache.samza.task.TaskContext;
import org.apache.samza.task.TaskCoordinator;
import org.apache.samza.task.sql.SimpleMessageCollector;

public class WriteSlidingWindowResultsOperator extends SimpleOperatorImpl {
  final RelProtoDataType protoRowType = new RelProtoDataType() {
    public RelDataType apply(RelDataTypeFactory a0) {
      return a0.builder()
          .add("rowtime", SqlTypeName.TIMESTAMP)
          .add("productId", SqlTypeName.INTEGER)
          .add("units", SqlTypeName.INTEGER)
          .add("unitsLastHour", SqlTypeName.INTEGER)
          .build();
    }
  };

  private final RelDataType type;
  private final Schema avroSchema;
  private final InsertToStreamSpec spec;

  private final SystemStream OUTPUT_STREAM = new SystemStream("kafka", "slidingwindowsqloutput");

  public WriteSlidingWindowResultsOperator(InsertToStreamSpec spec) {
    super(spec);
    this.spec = spec;
    this.type = protoRowType.apply(new JavaTypeFactoryImpl());
    this.avroSchema = AvroSchemaUtils.relDataTypeToAvroSchema(type);
  }

  @Override
  protected void realRefresh(long timeNano, SimpleMessageCollector collector, TaskCoordinator coordinator) throws Exception {

  }

  @Override
  protected void realProcess(Relation rel, SimpleMessageCollector collector, TaskCoordinator coordinator) throws Exception {

  }

  @Override
  protected void realProcess(Tuple tuple, SimpleMessageCollector collector, TaskCoordinator coordinator) throws Exception {
    collector.send(new OutgoingMessageEnvelope(OUTPUT_STREAM, tuple.getKey(),
        TupleConverter.objectArrayToSamzaData(((IntermediateMessageTuple)tuple).getContent(), type, avroSchema)));
  }

  @Override
  public void init(Config config, TaskContext context) throws Exception {

  }
}
