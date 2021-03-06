package org.openl.rules.calculation.result.convertor2.rules;

/*
 * #%L
 * OpenL - DEV - Rules - Calculation Result
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import org.apache.commons.lang3.ClassUtils;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.cast.JavaNoCast;
import org.openl.rules.calculation.result.convertor2.CalculationStep;
import org.openl.rules.convertor.ObjectToDataOpenCastConvertor;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.StringTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpreadsheetColumnExtractor<S extends CalculationStep> {

	private final Logger log = LoggerFactory.getLogger(SpreadsheetColumnExtractor.class);

	private String columnName;
	
	private RulesConfiguration<? extends CalculationStep, ? extends CalculationStep> rulesConfiguration;

	public String getColumnName() {
		return columnName;
	}
	
	public RulesConfiguration<? extends CalculationStep, ? extends CalculationStep> getRulesConfiguration() {
		return rulesConfiguration;
	}

	public SpreadsheetColumnExtractor(String columnName, RulesConfiguration<? extends CalculationStep, ? extends CalculationStep> rulesConfiguration) {
		this.columnName = columnName;
		this.rulesConfiguration = rulesConfiguration;
	}

	/**
	 * Convert the given value to the appropriate type that is expected. And
	 * store it to the row instance.
	 *
	 * @param valueForStoraging
	 * @param spreadsheetRow
	 *            for population with given data
	 */
	public void convertAndStoreData(Object valueForStoraging, S spreadsheetRow) {
		if (valueForStoraging != null) {
			Integer minConvertDistance = null;
			MappingInfo mappingInfoForStore = null;
			
			MappingInfo[] mappingInfos = rulesConfiguration.MappingData();
			
			for (MappingInfo mappingInfo : mappingInfos) {
				if (columnName.equals(mappingInfo.getColumnName())){
					Integer d = convertDistance(valueForStoraging, mappingInfo.getType());
					if (d != null && (minConvertDistance == null || d < minConvertDistance)) {
						minConvertDistance = d;
						mappingInfoForStore = mappingInfo;
					}
				}
			}
			if (mappingInfoForStore != null) {
				Object value = convert(valueForStoraging, mappingInfoForStore.getType());
				if (mappingInfoForStore.getType().isAssignableFrom(value.getClass())) {
					if (store(value, spreadsheetRow, mappingInfoForStore.getPropertyName(),
							mappingInfoForStore.getType())) {
						return;
					}
				}
			}
		}
	}

	private boolean store(Object value, S step, String propertyName, Class<?> expectedType) {
		Method setterMethod = getSetterMethod(step, propertyName, expectedType);
		if (setterMethod != null) {
			try {
				setterMethod.invoke(step, value);
				return true;
			} catch (Exception e) {
				log.warn(e.getMessage(), e);
			}
		} else {
			if (log.isWarnEnabled()) {
				log.warn("Cannot find setter in {} class for [{}] column", step.getClass().getName(),
						getColumnName());
			}
		}
		return false;
	}

	private Method getSetterMethod(S step, String propertyName, Class<?> expectedType) {
		Method setterMethod = null;
		// try to get setter, by upper case the first symbol in the column name,
		// and leave the other part as is
		//
		String setterName = StringTool.getSetterName(propertyName);
		try {
			setterMethod = step.getClass().getMethod(setterName, expectedType);
		} catch (Exception e) {
			try {
				// try to get setter by upper case the first symbol in the
				// column name, and lower
				// case the rest
				//
				setterName = getSetterName(propertyName);
				setterMethod = step.getClass().getMethod(setterName, expectedType);
			} catch (Exception e1) {
				log.warn(e1.getMessage(), e1);
			}
		}
		return setterMethod;
	}

	// protected for tests
	protected String getSetterName(String fieldName) {
		return String.format("set%s%s", fieldName.substring(0, 1).toUpperCase(), fieldName.substring(1).toLowerCase());
	}

	private Integer convertDistance(Object x, Class<?> expectedType) {
		if (x.getClass().isArray() && expectedType.isArray()) {
			IOpenCast openCast = ObjectToDataOpenCastConvertor.getConvertor(expectedType.getComponentType(),
					x.getClass().getComponentType());
			if (openCast != null) {
				return openCast.getDistance(JavaOpenClass.getOpenClass(expectedType.getComponentType()),
						JavaOpenClass.getOpenClass(x.getClass().getComponentType()));
			} else {
				return null;
			}
		} else {
			if (!ClassUtils.isAssignable(x.getClass(), expectedType)) {
				IOpenCast openCast = ObjectToDataOpenCastConvertor.getConvertor(expectedType, x.getClass());
				if (openCast != null) {
					return openCast.getDistance(JavaOpenClass.getOpenClass(expectedType),
							JavaOpenClass.getOpenClass(x.getClass()));
				} else {
					return null;
				}
			} else {
				return new JavaNoCast().getDistance(JavaOpenClass.getOpenClass(expectedType),
						JavaOpenClass.getOpenClass(x.getClass()));
			}
		}
	}

	private Object convert(Object x, Class<?> expectedType) {
		if (x.getClass().isArray() && expectedType.isArray()) {
			int length = Array.getLength(x);
			Object newValue = Array.newInstance(expectedType.getComponentType(), length);
			IOpenCast openCast = ObjectToDataOpenCastConvertor.getConvertor(expectedType.getComponentType(),
					x.getClass().getComponentType());
			for (int i = 0; i < length; i++) {
				Object componentValue = Array.get(x, i);
				if (componentValue != null && !ClassUtils.isAssignable(componentValue.getClass(), expectedType.getComponentType())) {
					try {
						componentValue = openCast.convert(componentValue);
					} catch (Exception e) {
						if (log.isDebugEnabled()) {
							log.debug("Cannot convert value {} to {}", componentValue,
									expectedType.getComponentType().getName(), e);
						}
						return x;
					}
				}
				Array.set(newValue, i, componentValue);
			}
			return newValue;
		} else {
			if (!ClassUtils.isAssignable(x.getClass(), expectedType)) {
				try {
					IOpenCast openCast = ObjectToDataOpenCastConvertor.getConvertor(expectedType, x.getClass());
					return openCast.convert(x);
				} catch (Exception e) {
					if (log.isDebugEnabled()) {
						log.debug("Cannot convert value {} to {}", x, expectedType.getName(), e);
					}
				}
			}
		}
		return x;
	}
}
