package com.aria.service;
import java.util.Map;
public interface CloudWorkspaceTransport {
 void validateOrigin(String origin);
 Map<String,Object> exchange(String origin,String secret,Map<String,Object> request);
}
