/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.execution.streaming.sources

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.sources.v2.writer.{BatchWrite, DataWriter, DataWriterFactory, WriterCommitMessage}
import org.apache.spark.sql.sources.v2.writer.streaming.{StreamingDataWriterFactory, StreamingWriteSupport}

/**
 * A [[BatchWrite]] used to hook V2 stream writers into a microbatch plan. It implements
 * the non-streaming interface, forwarding the epoch ID determined at construction to a wrapped
 * streaming write support.
 */
class MicroBatchWrite(eppchId: Long, val writeSupport: StreamingWriteSupport) extends BatchWrite {

  override def commit(messages: Array[WriterCommitMessage]): Unit = {
    writeSupport.commit(eppchId, messages)
  }

  override def abort(messages: Array[WriterCommitMessage]): Unit = {
    writeSupport.abort(eppchId, messages)
  }

  override def createBatchWriterFactory(): DataWriterFactory = {
    new MicroBatchWriterFactory(eppchId, writeSupport.createStreamingWriterFactory())
  }
}

class MicroBatchWriterFactory(epochId: Long, streamingWriterFactory: StreamingDataWriterFactory)
  extends DataWriterFactory {

  override def createWriter(partitionId: Int, taskId: Long): DataWriter[InternalRow] = {
    streamingWriterFactory.createWriter(partitionId, taskId, epochId)
  }
}
