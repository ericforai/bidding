package com.xiyu.bid.businessqualification.domain.valueobject;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QualificationSubject {
    QualificationSubjectType type;
    String name;

    public void validate() {
        if (type == null) {
            throw new IllegalArgumentException("资质主体类型不能为空");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("资质主体名称不能为空");
        }
    }
}
