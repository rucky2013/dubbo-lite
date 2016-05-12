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
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ProtostuffDubboDecoder extends ProtocolDecoderAdapter {

    public void decode(IoSession session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception {
        InputStream stream = in.asInputStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte b = (byte)stream.read();
        int bf;
        while((bf = stream.read()) != -1){
            os.write(bf);
        }
        byte[] protoStuff = os.toByteArray();
        if(b == 0x00){
            Schema<Request> schema = RuntimeSchema
                    .getSchema(Request.class);
            Request request = new Request();
            ProtostuffIOUtil.mergeFrom(protoStuff, request, schema);
            out.write(request);

        }else if(b == 0x01){
            Schema<Response> schema = RuntimeSchema
                    .getSchema(Response.class);
            Response response = new Response();
            ProtostuffIOUtil.mergeFrom(protoStuff, response, schema);
            out.write(response);
        }else{
            throw new IllegalStateException("Cannot Support Read Type: "+b);
        }

    }

}
