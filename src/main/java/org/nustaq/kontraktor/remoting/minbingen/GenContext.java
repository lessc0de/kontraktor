package org.nustaq.kontraktor.remoting.minbingen;

import org.nustaq.kontraktor.Callback;
import org.nustaq.serialization.FSTClazzInfo;

import java.util.Collection;
import java.util.Map;

/**
 * Created by ruedi on 27.05.2014.
 */
public class GenContext {

    public GenClazzInfo clazzInfos[];

    public String getJSTransform(FSTClazzInfo.FSTFieldInfo fi) {
	    Class fieldType = fi.getType();
	    return getJSTransform("this."+fi.getField().getName(), fieldType);
    }

	public String getJSTransform(String fieldName, Class fieldType) {
        String res = "";
		boolean isArray = fieldType.isArray();
		boolean isIntegral = fieldType.isPrimitive() || (isArray && fieldType.getComponentType().isPrimitive());
		if ( ! isIntegral && isArray) {
	        res += "MinBin.jarray("+ fieldName +")";
	    } else if ( Map.class.isAssignableFrom(fieldType) ) {
	        res += "MinBin.jmap("+ fieldName +")";
	    } else if ( Collection.class.isAssignableFrom(fieldType) ) {
	        res += "MinBin.jlist("+ fieldName +")";
	    } else {
		    if (isArray && isIntegral) {
			    int integralCode = FSTClazzInfo.FSTFieldInfo.getIntegralCode(fieldType.getComponentType());
			    switch (integralCode) {
		            case FSTClazzInfo.FSTFieldInfo.BOOL:
		                res += fieldName +"?1:0";
		                break;
		            case FSTClazzInfo.FSTFieldInfo.BYTE:
		                res += "MinBin.i8("+ fieldName +")";
		                break;
		            case FSTClazzInfo.FSTFieldInfo.CHAR:
		                res += "MinBin.ui16("+ fieldName +")";
		                break;
		            case FSTClazzInfo.FSTFieldInfo.SHORT:
		                res += "MinBin.i16("+ fieldName +")";
		                break;
		            case FSTClazzInfo.FSTFieldInfo.INT:
		                res += "MinBin.i32("+ fieldName +")";
		                break;
		            case FSTClazzInfo.FSTFieldInfo.LONG:
		                res += "MinBin.i64("+ fieldName +")";
		                break;
		            case FSTClazzInfo.FSTFieldInfo.FLOAT:
		                res += "MinBin.dbl("+ fieldName +")";
		                break;
		            case FSTClazzInfo.FSTFieldInfo.DOUBLE:
		                res += "MinBin.dbl("+ fieldName +")";
		                break;
		            default: throw new RuntimeException("wat? "+integralCode);
		        }
		    } else if ( fieldType == String[].class ) {
		        res += "MinBin.strArr("+ fieldName +")";
		    } else if ( isIntegral && !isArray) {
			    int integralCode = FSTClazzInfo.FSTFieldInfo.getIntegralCode(fieldType);
			    switch (integralCode) {
		            case FSTClazzInfo.FSTFieldInfo.BOOL:
		                res += fieldName +"?1:0";
		                break;
		            case FSTClazzInfo.FSTFieldInfo.BYTE:
                        res += "MinBin.parseIntOrNan("+ fieldName +", 'byte' )";
                        break;
		            case FSTClazzInfo.FSTFieldInfo.CHAR:
                        res += "MinBin.parseIntOrNan("+ fieldName +", 'char' )";
                        break;
		            case FSTClazzInfo.FSTFieldInfo.SHORT:
                        res += "MinBin.parseIntOrNan("+ fieldName +", 'short' )";
                        break;
		            case FSTClazzInfo.FSTFieldInfo.INT:
                        res += "MinBin.parseIntOrNan("+ fieldName +", 'int' )";
                        break;
		            case FSTClazzInfo.FSTFieldInfo.LONG:
		                res += "MinBin.parseIntOrNan("+ fieldName +", 'int' )";
		                break;
		            default:
                        res += fieldName;
		        }
		    } else if ( ! Number.class.isAssignableFrom(fieldType) &&
					    ! String.class.isAssignableFrom(fieldType) &&
                        ! Character.class.isAssignableFrom(fieldType) &&
                        ! Callback.class.isAssignableFrom(fieldType)
                     )
		    {
			    res += "MinBin.obj('"+fieldType.getSimpleName()+"',"+ fieldName + ")";
		    } else
			    res += fieldName;
	    }
		res += "";
		return res;
	}

}
