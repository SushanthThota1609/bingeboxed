package com.bingeboxed.shared.client;

import java.util.List;

public interface SocialGraphClient {
    List<Long> getFollowing(Long userId);
    List<Long> getFollowers(Long userId);
}
