/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.ballerinalang.stdlib.io.nativeimpl;

import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.jvm.values.connector.NonBlockingCallback;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.stdlib.io.channels.base.DataChannel;
import org.ballerinalang.stdlib.io.events.EventContext;
import org.ballerinalang.stdlib.io.events.EventRegister;
import org.ballerinalang.stdlib.io.events.EventResult;
import org.ballerinalang.stdlib.io.events.Register;
import org.ballerinalang.stdlib.io.events.data.ReadStringEvent;
import org.ballerinalang.stdlib.io.utils.IOConstants;
import org.ballerinalang.stdlib.io.utils.IOUtils;

/**
 * Extern function ballerina.io#readString.
 *
 * @since 0.974.1
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "io",
        functionName = "readString",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = "ReadableDataChannel",
                structPackage = "ballerina.io"),
        args = {@Argument(name = "nBytes", type = TypeKind.INT),
                @Argument(name = "encoding", type = TypeKind.STRING)},
        isPublic = true
)
public class ReadString {

    public static Object readString(Strand strand, ObjectValue dataChannelObj, long nBytes, String encoding) {
        DataChannel channel = (DataChannel) dataChannelObj.getNativeData(IOConstants.DATA_CHANNEL_NAME);
        EventContext eventContext = new EventContext(new NonBlockingCallback(strand));
        ReadStringEvent event = new ReadStringEvent(channel, eventContext, (int) nBytes, encoding);
        Register register = EventRegister.getFactory().register(event, ReadString::readChannelResponse);
        eventContext.setRegister(register);
        register.submit();
        return null;
    }

    /**
     * Triggers upon receiving the response.
     *
     * @param result the response received after reading int.
     * @return read int value.
     */
    private static EventResult readChannelResponse(EventResult<String, EventContext> result) {
        EventContext eventContext = result.getContext();
        Throwable error = eventContext.getError();
        NonBlockingCallback callback = eventContext.getNonBlockingCallback();
        if (null != error) {
            callback.setReturnValues(IOUtils.createError(error.getMessage()));
        } else {
            callback.setReturnValues(result.getResponse());
        }
        IOUtils.validateChannelState(eventContext);
        callback.notifySuccess();
        return result;
    }
}
