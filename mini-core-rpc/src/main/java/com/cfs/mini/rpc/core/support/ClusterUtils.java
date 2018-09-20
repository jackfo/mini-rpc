package com.cfs.mini.rpc.core.support;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;

import java.util.HashMap;
import java.util.Map;

public class ClusterUtils {

    private ClusterUtils() {
    }

    public static URL mergeUrl(URL remoteUrl, Map<String, String> localMap) {
        // 合并配置 Map 结果
        Map<String, String> map = new HashMap<String, String>();
        // 远程配置 Map 结果
        Map<String, String> remoteMap = remoteUrl.getParameters();

        // 添加 `remoteMap` 到 `map` 中，并移除不必要的配置
        if (remoteMap != null && remoteMap.size() > 0) {
            map.putAll(remoteMap);

            map.remove(Constants.THREAD_NAME_KEY);
            map.remove(Constants.DEFAULT_KEY_PREFIX + Constants.THREAD_NAME_KEY);

            map.remove(Constants.THREADPOOL_KEY);
            map.remove(Constants.DEFAULT_KEY_PREFIX + Constants.THREADPOOL_KEY);

            map.remove(Constants.CORE_THREADS_KEY);
            map.remove(Constants.DEFAULT_KEY_PREFIX + Constants.CORE_THREADS_KEY);

            map.remove(Constants.THREADS_KEY);
            map.remove(Constants.DEFAULT_KEY_PREFIX + Constants.THREADS_KEY);

            map.remove(Constants.QUEUES_KEY);
            map.remove(Constants.DEFAULT_KEY_PREFIX + Constants.QUEUES_KEY);

            map.remove(Constants.ALIVE_KEY);
            map.remove(Constants.DEFAULT_KEY_PREFIX + Constants.ALIVE_KEY);

            map.remove(Constants.TRANSPORTER_KEY);
            map.remove(Constants.DEFAULT_KEY_PREFIX + Constants.TRANSPORTER_KEY);
        }


        // 添加 `localMap` 到 `map` 中
        if (localMap != null && localMap.size() > 0) {
            map.putAll(localMap);
        }

        // 添加指定的 `remoteMap` 的配置项到 `map` 中，因为上面被 `localMap` 覆盖了。
        if (remoteMap != null && remoteMap.size() > 0) {
            // Use version passed from provider side
            String mini = remoteMap.get(Constants.MINI_VERSION_KEY);
            if (mini != null && mini.length() > 0) {
                map.put(Constants.MINI_VERSION_KEY, mini);
            }
            String version = remoteMap.get(Constants.VERSION_KEY);
            if (version != null && version.length() > 0) {
                map.put(Constants.VERSION_KEY, version);
            }
            String group = remoteMap.get(Constants.GROUP_KEY);
            if (group != null && group.length() > 0) {
                map.put(Constants.GROUP_KEY, group);
            }
            String methods = remoteMap.get(Constants.METHODS_KEY);
            if (methods != null && methods.length() > 0) {
                map.put(Constants.METHODS_KEY, methods);
            }
            // Reserve timestamp of provider url. 保留 provider 的启动 timestamp
            String remoteTimestamp = remoteMap.get(Constants.TIMESTAMP_KEY);
            if (remoteTimestamp != null && remoteTimestamp.length() > 0) {
                map.put(Constants.REMOTE_TIMESTAMP_KEY, remoteMap.get(Constants.TIMESTAMP_KEY));
            }
            // Combine filters and listeners on Provider and Consumer 合并 filter 和 listener
            String remoteFilter = remoteMap.get(Constants.REFERENCE_FILTER_KEY);
            String localFilter = localMap.get(Constants.REFERENCE_FILTER_KEY);
            if (remoteFilter != null && remoteFilter.length() > 0
                    && localFilter != null && localFilter.length() > 0) {
                localMap.put(Constants.REFERENCE_FILTER_KEY, remoteFilter + "," + localFilter);
            }
            String remoteListener = remoteMap.get(Constants.INVOKER_LISTENER_KEY);
            String localListener = localMap.get(Constants.INVOKER_LISTENER_KEY);
            if (remoteListener != null && remoteListener.length() > 0
                    && localListener != null && localListener.length() > 0) {
                localMap.put(Constants.INVOKER_LISTENER_KEY, remoteListener + "," + localListener);
            }
        }

        // 清空原有配置，使用合并的配置覆盖
        return remoteUrl.clearParameters().addParameters(map);
    }
}
