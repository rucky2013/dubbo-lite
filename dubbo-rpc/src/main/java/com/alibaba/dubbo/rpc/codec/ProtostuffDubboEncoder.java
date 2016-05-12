/**
 * Copyright 2002-2016 xiaoyuepeng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * @author xiaoyuepeng <xyp260466@163.com>
 */
package com.alibaba.dubbo.rpc.codec;

import com.alibaba.dubbo.rpc.remoting.Request;
import com.alibaba.dubbo.rpc.remoting.Response;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public class ProtostuffDubboEncoder extends ProtocolEncoderAdapter {

    /**
     * encode
     *
     * @param session
     * @param message
     * @param out
     * @throws Exception
     */
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4096);
        byteBuffer.setAutoExpand(true);
        if(message instanceof Request){
            byteBuffer.put((byte)0x00);
            Schema<Request> schema = RuntimeSchema
                    .getSchema(Request.class);
            LinkedBuffer buffer = LinkedBuffer.allocate(1024);
            byte[] protoStuff = ProtostuffIOUtil.toByteArray((Request)message, schema, buffer);
            byteBuffer.put(protoStuff);
        }else if(message instanceof Response){
            byteBuffer.put((byte)0x01);
            Schema<Response> schema = RuntimeSchema
                    .getSchema(Response.class);
            LinkedBuffer buffer = LinkedBuffer.allocate(4096);
            byte[] protoStuff = ProtostuffIOUtil.toByteArray((Response)message, schema, buffer);
            byteBuffer.put(protoStuff);
        }else{
            throw new IllegalStateException("Cannot Support Class Type: "+message.getClass().getName());
        }
        byteBuffer.flip();
        out.write(byteBuffer);
    }
}
