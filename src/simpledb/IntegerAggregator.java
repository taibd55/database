package simpledb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Knows how to compute some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {

	private int _groupField;
	private Type _fieldType;
	private int _aggregateField;
	private Op _op;
	private HashMap<Object, Integer> _data;
	private HashMap<Object, Integer> _keyCount;
		
	
    /**
     * Aggregate constructor
     * @param gbfield the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield the 0-based index of the aggregate field in the tuple
     * @param what the aggregation operator
     */

    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        _groupField = gbfield;
        _fieldType = gbfieldtype;
        _aggregateField = afield;
        _op = what;
        _data = new HashMap<Object, Integer>();
        _keyCount = new HashMap<Object, Integer>();
    }
    
    private int getInitValue() {
    	switch (_op) {
    	case AVG: return 0;
    	case COUNT: return 0;
    	case SUM: return 0;
    	case MIN: return Integer.MAX_VALUE;
    	case MAX: return Integer.MIN_VALUE;
    	default: assert (false);
    	}
    	
    	return 0;
    }
    
    private int getCurrentValue(Object key) {
    	if (!_data.containsKey(key)) {
    		int defaultValue = getInitValue();
    		_data.put(key, defaultValue);
    		_keyCount.put(key, 0);
    	}
    	
    	return _data.get(key);
    }
    
    private boolean isInteger(Field field) {
    	return field.getType() == Type.INT_TYPE;
    }
    
    private Object getKey(Tuple tuple) {
    	if (_groupField == Aggregator.NO_GROUPING) return null;
    	
    	Field field = tuple.getField(_groupField);
    	_fieldType = field.getType();
    	if (isInteger(field)) {
    		return ((IntField) field).getValue();
    	} else {
    		return ((StringField) field).getValue();
    	}
    }
   
    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
    	Object key = getKey(tup);
    	int value = ((IntField) tup.getField(_aggregateField)).getValue();
    	int aggregateValue = getCurrentValue(key);
    	
    	switch (_op) {
    	case SUM:
    	{
    		aggregateValue += value;
    		break;
    	}
    	case MIN:
    	{
    		if (aggregateValue > value) {
    			aggregateValue = value;
    		}
    		break;
    	}
    	case MAX:
    	{
    		if (aggregateValue < value) {
    			aggregateValue = value;
    		}
    		break;
    	}
    	case AVG:
    	{
    		aggregateValue += value;

    		int keyCount = _keyCount.get(key);
    		keyCount++;
    		_keyCount.put(key,  keyCount);
    		// Current number of keys * keyAvg == oldValue
    		// oldValue + avg / (keys + 1) == new Average
    		/*
    		int keyCount = _avgCount.get(key);
    		if (!hasAggregate()) {
    			keyCount = _data.size();
    		}
    		if (key instanceof Integer) {
    			int keyId = (Integer) key;
    			if (keyId == 0) {
    				System.out.println(keyCount + ": Adding to key 0: " + value);
    			}
    		}
    		
    		
    		// Have to use old # of keys to calculate previous total
    		int newAverage = aggregateValue * keyCount;
    		newAverage += value;
    		
    		// Have to include this new "key" in calculating the average
    		++keyCount;
    		aggregateValue = newAverage / keyCount;
    		if (key instanceof Integer) {
    			int keyId = (Integer) key;
    			if (keyId == 0) {
    				System.out.println(keyCount + ": Aggregate: " + aggregateValue);
    			}
    		}
    		
    		_avgCount.put(key, keyCount);
    		*/
    		break;
    	}
    	default:
    		assert (false);
    	}
    	
    	_data.put(key,  aggregateValue);
    }
    
    private boolean hasAggregate() {
    	return _groupField != Aggregator.NO_GROUPING;
    }
    
    private Field getGroupField(Object key) {
    	if (!hasAggregate()) {
    		return new IntField(0);
    	} else if (_fieldType == Type.INT_TYPE) {
    		return new IntField((Integer) key);
		} else {
			return new StringField((String) key, _fieldType.getLen());
		}
    }
    
    private TupleDesc createTupleDesc() {
    	if (hasAggregate()) {
    		Type[] types = new Type[2];
    		_fieldType = Type.INT_TYPE;
    		types[0] = this._fieldType;    	
        	types[1] = Type.INT_TYPE;
        	
        	String[] names = new String[2];
        	names[0] = "key";
        	names[0] = _op.toString();
        	return new TupleDesc(types, names);
    	} else {
    		Type[] types = new Type[1];
    		types[0] = Type.INT_TYPE;
    		return new TupleDesc(types);
    	}
    }

    /**
     * Create a DbIterator over group aggregate results.
     *
     * @return a DbIterator whose tuples are the pair (groupVal,
     *   aggregateVal) if using group, or a single (aggregateVal) if no
     *   grouping. The aggregateVal is determined by the type of
     *   aggregate specified in the constructor.
     */
    public DbIterator iterator() {
    	TupleDesc description = createTupleDesc();
    	ArrayList<Tuple> results = new ArrayList<Tuple>();
    	
    	for (Object key : _data.keySet()) {
    		int value = _data.get(key);
    		
    		Tuple newTuple = new Tuple(description);
    		Field groupBy = getGroupField(key);
    		if (_op == Aggregator.Op.AVG) {
    			value = value / _keyCount.get(key);
    		}
    		
    		Field aggregate = new IntField(value);
    		if (hasAggregate()) {
    			newTuple.setField(0, groupBy);
    			newTuple.setField(1, aggregate);
    		} else {
    			newTuple.setField(0, aggregate);
    		}
    		
    		results.add(newTuple);
    	}
    	
    	return new TupleIterator(description, results);
    }

}
