/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth2.internal;

import org.wso2.carbon.identity.oauth.OAuthUtil;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dao.OAuthTokenPersistenceFactory;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;
import org.wso2.carbon.identity.oauth2.model.AuthzCodeDO;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;
import org.wso2.carbon.identity.user.store.configuration.listener.AbstractUserStoreConfigListener;
import org.wso2.carbon.user.api.UserStoreException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OAuthUserStoreConfigListenerImpl extends AbstractUserStoreConfigListener {
    @Override
    public void onUserStoreNamePreUpdate(int tenantId, String currentUserStoreName, String newUserStoreName) throws
            UserStoreException {
        try {
            Set<AccessTokenDO> accessTokenDOs = OAuthTokenPersistenceFactory.getInstance()
                    .getAccessTokenDAO().getAccessTokensOfUserStore(tenantId,
                    currentUserStoreName);
            for (AccessTokenDO accessTokenDO : accessTokenDOs) {
                //Clear cache
                OAuthUtil.clearOAuthCache(accessTokenDO.getConsumerKey(), accessTokenDO.getAuthzUser(),
                        OAuth2Util.buildScopeString(accessTokenDO.getScope()));
                OAuthUtil.clearOAuthCache(accessTokenDO.getConsumerKey(), accessTokenDO.getAuthzUser());
                OAuthUtil.clearOAuthCache(accessTokenDO.getAccessToken());
            }
            OAuthTokenPersistenceFactory.getInstance().getAccessTokenDAO().updateUserStoreDomain(tenantId,
                    currentUserStoreName,
                    newUserStoreName);
            OAuthTokenPersistenceFactory.getInstance()
                    .getAuthorizationCodeDAO().updateUserStoreDomain(tenantId,
                    currentUserStoreName, newUserStoreName);
        } catch (IdentityOAuth2Exception e) {
            throw new UserStoreException("Error occurred while renaming user store : " + currentUserStoreName +
                    " in tenant :" + tenantId, e);
        }
    }

    @Override
    public void onUserStorePreDelete(int tenantId, String userStoreName) throws UserStoreException {
        try {
            Set<AccessTokenDO> accessTokenDOs = OAuthTokenPersistenceFactory.getInstance()
                    .getAccessTokenDAO().getAccessTokensOfUserStore(tenantId, userStoreName);
            Map<String, AccessTokenDO> latestAccessTokens = new HashMap<>();
            for (AccessTokenDO accessTokenDO : accessTokenDOs) {
                String keyString = accessTokenDO.getConsumerKey() + ":" + accessTokenDO.getAuthzUser() + ":" +
                        OAuth2Util.buildScopeString(accessTokenDO.getScope());
                AccessTokenDO accessTokenDOFromMap = latestAccessTokens.get(keyString);
                if (accessTokenDOFromMap != null) {
                    if (accessTokenDOFromMap.getIssuedTime().before(accessTokenDO.getIssuedTime())) {
                        latestAccessTokens.put(keyString, accessTokenDO);
                    }
                } else {
                    latestAccessTokens.put(keyString, accessTokenDO);
                }

                //Clear cache
                OAuthUtil.clearOAuthCache(accessTokenDO.getConsumerKey(), accessTokenDO.getAuthzUser(),
                        OAuth2Util.buildScopeString(accessTokenDO.getScope()));
                OAuthUtil.clearOAuthCache(accessTokenDO.getConsumerKey(), accessTokenDO.getAuthzUser());
                OAuthUtil.clearOAuthCache(accessTokenDO.getAccessToken());
            }
            ArrayList<String> tokensToRevoke = new ArrayList<>();
            for (Map.Entry entry : latestAccessTokens.entrySet()) {
                tokensToRevoke.add(((AccessTokenDO) entry.getValue()).getAccessToken());
            }
            OAuthTokenPersistenceFactory.getInstance().getAccessTokenDAO()
                    .revokeAccessTokens(tokensToRevoke.toArray(new String[tokensToRevoke.size()]));
            List<AuthzCodeDO> latestAuthzCodes = OAuthTokenPersistenceFactory.getInstance()
                    .getAuthorizationCodeDAO().getLatestAuthorizationCodesByUserStore(tenantId, userStoreName);
            for (AuthzCodeDO authzCodeDO : latestAuthzCodes) {
                // remove the authorization code from the cache
                OAuthUtil.clearOAuthCache(authzCodeDO.getConsumerKey() + ":" + authzCodeDO.getAuthorizationCode());

            }
            OAuthTokenPersistenceFactory.getInstance()
                    .getAuthorizationCodeDAO().deactivateAuthorizationCodes(latestAuthzCodes);
        } catch (IdentityOAuth2Exception e) {
            throw new UserStoreException("Error occurred while revoking Access Token of user store : " +
                    userStoreName + " in tenant :" + tenantId, e);
        }
    }
}
