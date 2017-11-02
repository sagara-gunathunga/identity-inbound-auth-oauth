/*
 *
 *   Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 * /
 */

package org.wso2.carbon.identity.oauth2.dao;

public class OAuthTokenPersistenceFactory {

    private static OAuthTokenPersistenceFactory factory;
    private AuthorizationCodeDAO authorizationCodeDAO;
    private AccessTokenDAO tokenDAO;
    private OAuthScopeDAO scopeDAO;
    private TokenManagementDAO managementDAO;

    public OAuthTokenPersistenceFactory() {

        this.authorizationCodeDAO = new AuthorizationCodeDAOImpl();
        this.tokenDAO = new AccessTokenDAOImpl();
        this.scopeDAO = new OAuthScopeDAOImpl();
        this.managementDAO = new TokenManagementDAOImpl();
    }

    public static OAuthTokenPersistenceFactory getInstance() {

        if (factory == null) {
            factory = new OAuthTokenPersistenceFactory();
        }
        return factory;
    }

    public AuthorizationCodeDAO getAuthorizationCodeDAO() {

        return authorizationCodeDAO;
    }

    public AccessTokenDAO getAccessTokenDAO() {

        return tokenDAO;
    }

    public OAuthScopeDAO getOAuthScopeDAO() {

        return scopeDAO;
    }

    public TokenManagementDAO getTokenManagementDAO() {

        return managementDAO;
    }
}
