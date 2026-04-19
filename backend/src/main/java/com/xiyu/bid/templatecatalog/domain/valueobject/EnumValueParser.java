package com.xiyu.bid.templatecatalog.domain.valueobject;

final class EnumValueParser {

    private EnumValueParser() {
    }

    static <E extends Enum<E>> E parse(Class<E> enumClass, String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (E constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(value)) {
                return constant;
            }
            if (constant instanceof ProductType productType && productType.getLabel().equalsIgnoreCase(value)) {
                return enumClass.cast(productType);
            }
            if (constant instanceof IndustryType industryType && industryType.getLabel().equalsIgnoreCase(value)) {
                return enumClass.cast(industryType);
            }
            if (constant instanceof DocumentType documentType && documentType.getLabel().equalsIgnoreCase(value)) {
                return enumClass.cast(documentType);
            }
        }

        throw new IllegalArgumentException("不支持的" + label + ": " + value);
    }
}
