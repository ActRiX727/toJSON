package frog.json;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.sun.xml.internal.txw2.output.CharacterEscapeHandler;

/**
 * 转换JSON字符串的工具类，支持自定义类、包装类、集合、String类、Date类，
 * 输出默认为无格式，调用重载的方法来使输出的格式阅读性好
 * @author guoxinlu
 */
public class ToJSON {

	//Object类型类型起始字符
	private static final String OBJ_START     = "{";
	
	//Object类型类型结束字符
	private static final String OBJ_END       = "}";
	
	//数组类型类型起始字符
	private static final String ARRAY_START   = "[";
	
	//数组类型类型结束字符
	private static final String ARRAY_END     = "]";
	
	//key-value/field-fieldValue分割符
	private static final String KEY_SPLIT     = ":";
	
	//元素分割符
	private static final String ELEM_SPLIT    = ",";
	
	//日期格式默认yyyy-MM-dd HH:mm:ss
	private static String dateFormat = "yyyy-MM-dd HH:mm:ss";

	//设置日期格式
	public static void setDateFormat(String dateFormat) {
		ToJSON.dateFormat = dateFormat;
	}
	
	//日期按格式输出的字符串
	public static String dateToString(Date date){
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		return sdf.format(date);
	}
		
	//Object类型(对象\Map)拼接的开始
	private static StringBuffer objStart(){
		return new StringBuffer(OBJ_START);
	}
	
	//Object类型(对象\Map)拼接的结束
	private static String objEnd(StringBuffer sb){
		if(sb.lastIndexOf(ELEM_SPLIT) != -1){
			sb.deleteCharAt(sb.lastIndexOf(ELEM_SPLIT));
		}
		return sb.append(OBJ_END).toString();
	}
	
	//Array类型(集合\数组)拼接的开始
	private static StringBuffer arrayStart(){
		return new StringBuffer(ARRAY_START);
	}
	
	//Array类型(集合\数组)拼接的结束
	private static String arrayEnd(StringBuffer sb){
		if(sb.lastIndexOf(ELEM_SPLIT) != -1){
			sb.deleteCharAt(sb.lastIndexOf(ELEM_SPLIT));
		}
		return sb.append(ARRAY_END).toString();
	}
	
	//拼接Map中的key/Object中的属性名---->"key":
	private static String appendKey(String key){
		StringBuffer sb = new StringBuffer();
		return sb.append("\"").append(key).append("\"").append(KEY_SPLIT).toString();
	}
	
	//该方法已被appendValue(obj)替代
	//拼接Map中的value/Object中的属性值
	private static String appendValue2(Object obj){
		if(obj == null){
			return null;
		}
		StringBuffer sb = objStart();
		//获取对象属性
		Class<? extends Object> oClass = obj.getClass();
		Field[] fields = oClass.getDeclaredFields();
		//判断对象有没有属性
		if(fields.length <= 0){
			sb.append("null");
		}else{
			for (Field field : fields) {
				
				//获取私有属性，有待修改！！！  //序列化问题
				field.setAccessible(true);
				
				Object fieldValue = null;
				try {
					fieldValue = field.get(obj);//获取属性值
				} catch (Exception e) {
					e.printStackTrace();
				}
				String fieldName = field.getName();//属性名
				//append fieldName
				sb.append(appendKey(fieldName));
				//append fieldValue
				sb.append(objectToJSON(fieldValue));
				sb.append(ELEM_SPLIT);
			}

		}
		return objEnd(sb);
	}
	
	//拼接Map中的value/Object中的属性值
	private static String appendValue(Object obj){
		
		if(obj == null){
			return null;
		}
		
		StringBuffer sb = objStart();
		
		//获取对象属性
		Class<? extends Object> oClass = obj.getClass();
		Field[] fields = oClass.getDeclaredFields();
		
		//判断对象有没有属性
		if(fields.length <= 0){
			sb.append("null");
		}else{
			for (Field field : fields) {
				
				//获取私有属性
				field.setAccessible(true);
				String fieldName = field.getName();//属性名
				
				//判断有没有get方法
				//1 字符串首字母大写
				char[] ch = fieldName.toCharArray();
				if(ch[0] >= 'a' && ch[0] <= 'z'){
					ch[0] = (char)(ch[0] - 32);
				}
				String str = "get" + new String(ch);
				
				//2 判断方法名存在
				Method method = null;
				try {
					//获取无参的公开方法
					method = oClass.getMethod(str, null);
				} catch (Exception e1) {
					//e1.printStackTrace();
					//异常处理！！！
					throw new RuntimeException("没有方法:"+str+"()  "+e1.getLocalizedMessage());
				}
				
				//该属性有get方法
				if(method != null){
					//3 判断方法修饰符是否为公开方法
					if(Modifier.isPublic(method.getModifiers())){
						//获取属性值
						Object fieldValue = null;
						try {
							fieldValue = field.get(obj);//获取属性值
						} catch (Exception e) {
							e.printStackTrace();
						}
						
						//append fieldName
						sb.append(appendKey(fieldName));
						//append fieldValue
						sb.append(objectToJSON(fieldValue));
						sb.append(ELEM_SPLIT);
					}
				}
				
			}

		}
		return objEnd(sb);
	}

	/**
	 * Collection to Json
	 * (List集合、Set集合转换为Json字符串)
	 * @param collection (List or Set)
	 * @return [Object1,Object2, ...]
	 */
	public static String collectionToJSON(Collection<?> collection){
		if(collection == null){
			return null;
		}
		StringBuffer sb = arrayStart();
		//遍历集合中元素
		for (Object object : collection) {
			//append object by type
			sb.append(objectToJSON(object));
			sb.append(ELEM_SPLIT);
		}
		return arrayEnd(sb);
	}
	
	/**
	 * Collection to Json
	 * (List集合、Set集合转换为Json字符串)
	 * @param collection (List or Set)
	 * @param boo (true is format)
	 * @return [Object1,Object2, ...]
	 */
	public static String collectionToJSON(Collection<?> collection, boolean boo){
		String collectionToJSON = collectionToJSON(collection);
		if(boo){
			return jsonFormat(collectionToJSON);
		}
		return collectionToJSON;
	}
	
	/**
	 * array to Json (Object[]转换为json串)
	 * @param oobj (Object[])
	 * @return [{"name":"jb","age":25},{"name":"xl","age":24},...]
	 */
	public static String arrayToJSON(Object[] oobj){
		//oobj为空直接返回null
		if(oobj == null){
			return null;
		}
		StringBuffer sb = arrayStart();
		for (Object object : oobj) {
			sb.append(objectToJSON(object));
			sb.append(ELEM_SPLIT);
		}
		return arrayEnd(sb);
	}
	
	/**
	 * array to Json (Object[]转换为json串)
	 * @param oobj (Object[])
	 * @param boo (jsonFormat)
	 * @return [{"name":"jb","age":25},{"name":"xl","age":24},...]
	 */
	public static String arrayToJSON(Object[] oobj, boolean boo){
		String arrayToJSON = arrayToJSON(oobj);
		if(boo){
			return jsonFormat(arrayToJSON);
		}
		return arrayToJSON;
	}
	
	/**
	 * Object to Json (对象转换为json串)
	 * @param obj
	 * @return {"name":"frog","age":20,"birthday":"1999-10-10 15:50:51", ...}
	 */
	public static String objectToJSON(Object obj) {
		
		if(obj == null){
			return null;
		}
		
		//获取obj对象的Class对象
		Class<? extends Object> oClass = obj.getClass();
		
		StringBuffer sb = new StringBuffer();
		
		//根据类型做不同处理
		if(String.class.isAssignableFrom(oClass)){
			//String
			//解决不转义
			String string = charSequence(obj);
			sb.append("\"").append(string).append("\"");
		}else if(Map.class.isAssignableFrom(oClass)){
			//Map
			sb.append(mapToJSON((Map<String,Object>) obj));
		}else if(Collection.class.isAssignableFrom(oClass)){
			//Collection
			sb.append(collectionToJSON((Collection<Object>) obj));
		}else if(Integer.class.isAssignableFrom(oClass) || 
				 Double.class.isAssignableFrom(oClass)  || 
				 Boolean.class.isAssignableFrom(oClass) || 
				 Long.class.isAssignableFrom(oClass)    || 
				 Byte.class.isAssignableFrom(oClass)    || 
				 Short.class.isAssignableFrom(oClass)   || 
				 Float.class.isAssignableFrom(oClass)){
			//7种基本类型对应的包装类型
			sb.append(obj);
		}else if(Character.class.isAssignableFrom(oClass)){
			//Character类型，输出ascii码?
			//int cha = ((Character)obj).charValue();
			//解决不转义
			String string = charSequence(obj);
			sb.append("\"").append(string).append("\"");
			//转为字符串 or 16进制?
		}else if(oClass.isArray()){
			//数组类型 
			sb.append(arrayToJSON((Object[])obj));
		}else if(Date.class.isAssignableFrom(oClass)){
			//Date类型
			sb.append("\"").append(dateToString((Date)obj)).append("\"");
		}else{
			//其他自定义类型
			sb.append(appendValue(obj));
		}
		
		return sb.toString();
	}
	
	/**
	 * 转义字符的处理
	 * @param obj
	 * @return
	 */
	private static String charSequence(Object obj){
		String string = null;
		if(obj instanceof Character){
			Character c = (Character) obj;
  			string = String.valueOf(c);
		}else {
			string = (String) obj;
		}
		
		StringBuffer sb = new StringBuffer();
		
		for(int i = 0; i < string.length(); i++){
			char c = string.charAt(i);
			switch (c) {
			case '\"':
				sb.append("\\\"");
				break;
			case '\\':
				sb.append("\\\\");
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			case '\'':
				sb.append("\\'");
				break;
			default:
				sb.append(c);
				break;
			}
		}
		return sb.toString();
	}
	
	/**
	 * Object to Json (对象转换为json串)
	 * @param obj
	 * @return {"name":"frog","age":20,"birthday":"1999-10-10 15:50:51", ...}
	 */
	public static String objectToJSON(Object obj, boolean boo){
		String objectToJSON = objectToJSON(obj);
		if(boo){
			return jsonFormat(objectToJSON);
		}
		return objectToJSON;
	}
	
	/**
	 * Map to Json (Map转换为json串)
	 * @param obj
	 * @return {"key":"value", ...}
	 */
	public static String mapToJSON(Map<String,Object> map){
		//map为空直接返回null
		if(map == null){
			return null;
		}
		//append start
		StringBuffer sb = ToJSON.objStart();
		//遍历map
		Set<Entry<String,Object>> entrySet = map.entrySet();
		for (Entry<String, Object> entry : entrySet) {
			//append key
			sb.append(ToJSON.appendKey(entry.getKey()));
			//获取map中value
			//append value
			sb.append(ToJSON.objectToJSON((entry.getValue())));
			sb.append(ELEM_SPLIT);
		}
		return objEnd(sb);
	}
	
	/**
	 * Map to Json (Map转换为json串)
	 * @param obj
	 * @return {"key":"value", ...}
	 */
	public static String mapToJSON(Map<String,Object> map, boolean boo){
		String mapToJSON = mapToJSON(map);
		if(boo){
			return jsonFormat(mapToJSON);
		}
		return mapToJSON;
	}
	
	/**
	 * 格式化json串
	 * @param json
	 * @return json
	 */
	public static String jsonFormat(String json){
		StringBuffer sb = new StringBuffer();
		int tabNum = 0;
		for(int index = 0; index < json.length(); index++){
			char c = json.charAt(index);
			if(tabNum > 0 && '\n' == sb.charAt(sb.length()-1)){
				sb.append(getLevelStr(tabNum));
			}
			switch (c) {
			case '{':
			case '[':
				sb.append(c + "\n");
				tabNum++;
				break;
			case ',':
				sb.append(c + "\n");
				break;
			case '}':
			case ']':
				sb.append("\n");
				tabNum--;
				sb.append(getLevelStr(tabNum));
				sb.append(c);
				break;
			default:
				sb.append(c);
				break;
			}
		}
		return sb.toString();
	}
	private static String getLevelStr(int tabNum){
		StringBuffer sb = new StringBuffer();
		for(int index = 0; index < tabNum; index++){
			sb.append("\t");
		}
		return sb.toString();
	}
}
