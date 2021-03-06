package com.ks.bean;

import com.google.common.base.Optional;
import com.ks.error.KConfigException;
import static com.ks.process.KUtils.*;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.KTable;

import java.util.Properties;

/**
 * com.ks.name kSource名称
 * <p>
 * com.ks.type kSource类型(stream,table),默认stream
 * <p>
 * com.ks.topics partition等属性一致的一类topic,暂时单个
 * <p>
 * com.ks.table.store type是table时,提供自定义的storeName
 * <p>
 * com.ks.time.name 事件时间字段,默认kafka record的timestamp.
 * <p>
 * com.ks.time.type 时间值类型long或string(string需要配置format)
 * <p>
 * com.ks.time.format 时间值的字符串格式
 * <p>
 * com.ks.time.lang 时间值值的语言,默认en
 * <p>
 * com.ks.time.offsetId 时间值的时区,默认东八区
 */
public class KSource {

    /**
     * configuration definition
     */
    private enum CONFIG {
        KS_NAME("ks.name"), KS_TYPE("ks.type"), KS_TOPICS("ks.topics"), KS_TABLE_STORE("ks.table.store"),
        KS_TIME_NAME("ks.time.name"), KS_TIME_TYPE("ks.time.type"), KS_TIME_FORMAT("ks.time.format"),
        KS_TIME_LANG("ks.time.lang"), KS_TIME_OFFSET_ID("ks.time.offsetId");
        private String value;

        CONFIG(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private final String ksName;
    private final String ksType;
    private final String ksTopics;
    private final String ksTableStore;
    private final String ksTimeName;
    private final String ksTimeType;
    private final String ksTimeFormat;
    private final String ksTimeLang;
    private final String ksTimeOffsetId;
    private final KStreamBuilder kStreamBuilder;

    public KSource(Properties properties, KStreamBuilder kStreamBuilder) {
        this.kStreamBuilder = kStreamBuilder;
        this.ksName = nonNullEmpty(properties, CONFIG.KS_NAME.getValue());
        this.ksType = nonNullEmpty(properties, CONFIG.KS_TYPE.getValue());
        String _tableStore = properties.getProperty(CONFIG.KS_TABLE_STORE.getValue());
        this.ksTableStore = isNullOrEmpty(_tableStore) ? null : _tableStore;
        this.ksTopics = nonNullEmpty(properties, CONFIG.KS_TOPICS.getValue());
        this.ksTimeName = properties.getProperty(CONFIG.KS_TIME_NAME.getValue());
        this.ksTimeType = isNullOrEmpty(this.ksTimeName) ?
                properties.getProperty(CONFIG.KS_TIME_TYPE.getValue()) :
                nonNullEmpty(properties, CONFIG.KS_TIME_TYPE.getValue());
        if (!isNullOrEmpty(this.ksTimeType) &&
                KTime.Type.STRING.getValue().equals(this.ksTimeType)) {
            this.ksTimeFormat = nonNullEmpty(properties, CONFIG.KS_TIME_FORMAT.getValue());
        } else this.ksTimeFormat = properties.getProperty(CONFIG.KS_TIME_FORMAT.getValue());
        this.ksTimeLang = properties.getProperty(CONFIG.KS_TIME_LANG.getValue());
        this.ksTimeOffsetId = properties.getProperty(CONFIG.KS_TIME_OFFSET_ID.getValue());
    }

    /**
     * get kStream or kTable
     *
     * @return kStream, kTable {@link KStream,KTable}
     */
    public Object source() {
        Optional<KTime> eventTime = new KTime.Builder()
                .name(ksTimeName).type(ksTimeType).format(ksTimeFormat).lang(ksTimeLang).offsetId(ksTimeOffsetId)
                .build();
        switch (ksType) {
            case "table":
                if (eventTime.isPresent()) {
                    return kStreamBuilder.table(new KTimestampExtractor(eventTime.get()),
                            Serdes.String(), Serdes.String(), ksTopics, ksTableStore);
                } else return kStreamBuilder.table(ksTopics, ksTableStore);
            case "stream":
                if (eventTime.isPresent()) {
                    return kStreamBuilder.stream(new KTimestampExtractor(eventTime.get()),
                            Serdes.String(), Serdes.String(), ksTopics);
                } else return kStreamBuilder.stream(ksTopics);
            default:
                throw new KConfigException(concat(" ", "kSource type'", ksType, "'not support..."));
        }
    }

    /**
     * kSource name
     *
     * @return ksName
     */
    public String getKsName() {
        return ksName;
    }
}
