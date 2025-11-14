package com.xiaochen.mianshiya.blackfilter;

import cn.hutool.bloomfilter.BitMapBloomFilter;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

/**
 * 黑名单过滤工具类
 */
@Slf4j
public class BlackIpUtil {
    private static BitMapBloomFilter bitMapBloomFilter;
    public static boolean isBlackIp(String ip){
        return bitMapBloomFilter.contains(ip);
    }

    public static void rebuildBlackIp(String configIp) {
        if (StrUtil.isBlank(configIp)) {
            configIp = "{}";
        }
        Yaml yaml = new Yaml();
        Map<String, List<String>> map = yaml.loadAs(configIp, Map.class);
        List<String> list = map.get("blackIpList");
        synchronized (BlackIpUtil.class) {
            if (CollUtil.isNotEmpty(list)) {
                bitMapBloomFilter = new BitMapBloomFilter(958506);
                for (String blackIp : list) {
                    bitMapBloomFilter.add(blackIp);
                }
            } else bitMapBloomFilter = new BitMapBloomFilter(100);
        }
    }
}
