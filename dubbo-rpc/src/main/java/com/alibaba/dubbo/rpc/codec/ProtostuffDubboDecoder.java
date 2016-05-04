/*
 * Dubbo Mina Decoder Use Protostuff
 *
 *
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
