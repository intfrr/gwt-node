/*
 * Copyright 2011 Chad Retz
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.gwtnode.core.node.tls;

import org.gwtnode.core.JavaScriptFunctionArguments;
import org.gwtnode.core.JavaScriptReturningFunctionWrapper;
import org.gwtnode.core.node.crypto.SecureContext;

/**
 * @author Chad Retz
 */
public abstract class SniCallback extends JavaScriptReturningFunctionWrapper<SecureContext> {

    @Override
    public SecureContext call(JavaScriptFunctionArguments args) {
        return onCallback((String) args.get(0));
    }

    protected abstract SecureContext onCallback(String servername); 
}