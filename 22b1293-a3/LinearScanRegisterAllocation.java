import java.util.*;

public class LinearScanRegisterAllocation {
    List<Map.Entry<String, Range>> list;
    List<Map.Entry<String, Range>> active;
    Set<String> freeReg = new TreeSet<>();
    Map<String, String> registerAllocation;
    Set<String> formalParametersSet;
    public LinearScanRegisterAllocation(List<Map.Entry<String, Range>> list, int numRegister, Set<String> formalParametersSet) {
        this.list = list;
        active = new ArrayList<>();
        for(int i = 0; i < numRegister; i++){
            String regname = "R" + i;
            freeReg.add(regname);
        }
        registerAllocation = new HashMap<>();
        this.formalParametersSet = formalParametersSet;
    }
import java.util.*;

public class LinearScanRegisterAllocation {
    List<Map.Entry<String, Range>> list;
    List<Map.Entry<String, Range>> active;
    Set<String> freeReg = new TreeSet<>();
    Map<String, String> registerAllocation;
    Set<String> formalParametersSet;
    public LinearScanRegisterAllocation(List<Map.Entry<String, Range>> list, int numRegister, Set<String> formalParametersSet) {
        this.list = list;
        active = new ArrayList<>();
        for(int i = 0; i < numRegister; i++){
            String regname = "R" + i;
            freeReg.add(regname);
        }
        registerAllocation = new HashMap<>();
        this.formalParametersSet = formalParametersSet;
    }

    public Map<String,String> analyse(){
        if (freeReg.isEmpty()){
            for (Map.Entry<String,Range> entry: list){
                if (!formalParametersSet.contains(entry.getKey())) {
                    registerAllocation.put(entry.getKey(), "spill");
                }
            }
        }else{
            for (Map.Entry<String,Range> entry: list){
                ExpireOldIntervals(entry);
                if (!formalParametersSet.contains(entry.getKey())){
                    if (freeReg.isEmpty()){
                        SpillAtInterval(entry);
                    } else{
                        String register = freeReg.stream().findFirst().orElse(null);
                        registerAllocation.put(entry.getKey(), register);
                        freeReg.remove(register);
                        InsertIntoActive(entry);
                    }
                }
            }
        }

        return registerAllocation;
    }

    private void InsertIntoActive(Map.Entry<String,Range> entry) {
        int i = 0;
        for(i = 0; i < active.size(); i++){
            if (active.get(i).getValue().getEnd() > entry.getValue().getEnd()){
                break;
            }
        }
        active.add(i,entry);
    }

    private void SpillAtInterval(Map.Entry<String,Range> entry) {
//        if (active.isEmpty()){
//            registerAllocation.put(entry.getKey(), "spill");
//        }
        Map.Entry<String,Range> spill = active.getLast();
        if (entry.getValue().getEnd() > spill.getValue().getEnd()){
            // spill the entry
            registerAllocation.put(entry.getKey(), "spill");
        } else if(entry.getValue().getEnd() == spill.getValue().getEnd()){
            if (entry.getKey().compareTo(spill.getKey()) < 0){
                registerAllocation.put(entry.getKey(), "spill");
            } else{
                registerAllocation.put(entry.getKey(),registerAllocation.get(spill.getKey()));
                registerAllocation.put(spill.getKey(), "spill");
                active.removeLast();
                active.add(entry);
            }
        }else{
            // spill the last active
            String register = registerAllocation.get(spill.getKey());
            registerAllocation.put(spill.getKey(), "spill");
            registerAllocation.put(entry.getKey(), register);
            active.remove(spill);
            InsertIntoActive(entry);
        }
    }

//    private void ExpireOldIntervals(Map.Entry<String,Range> entry) {
//        for (Map.Entry<String,Range> entry1: active){
//            if (entry1.getValue().getEnd() <= entry.getValue().getStart()){
//                active.remove(entry1);
//                freeReg.add(registerAllocation.get(entry1.getKey()));
//            } else{
//                return;
//            }
//        }
//    }

    private void ExpireOldIntervals(Map.Entry<String,Range> entry) {
        // Use iterator to safely remove elements during iteration
        Iterator<Map.Entry<String, Range>> iterator = active.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Range> activeEntry = iterator.next();
            if (activeEntry.getValue().getEnd() <= entry.getValue().getStart()) {
                // Free the register
                freeReg.add(registerAllocation.get(activeEntry.getKey()));
                // Remove from active list using iterator
                iterator.remove();
            } else {
                // Since active is sorted by end time, we can break early
                break;
            }
        }
    }
}

    public Map<String,String> analyse(){
        if (freeReg.isEmpty()){
            for (Map.Entry<String,Range> entry: list){
                if (!formalParametersSet.contains(entry.getKey())) {
                    registerAllocation.put(entry.getKey(), "spill");
                }
            }
        }else{
            for (Map.Entry<String,Range> entry: list){
                ExpireOldIntervals(entry);
                if (!formalParametersSet.contains(entry.getKey())){
                    if (freeReg.isEmpty()){
                        SpillAtInterval(entry);
                    } else{
                        String register = freeReg.stream().findFirst().orElse(null);
                        registerAllocation.put(entry.getKey(), register);
                        freeReg.remove(register);
                        InsertIntoActive(entry);
                    }
                }
            }
        }

        return registerAllocation;
    }

    private void InsertIntoActive(Map.Entry<String,Range> entry) {
        int i = 0;
        for(i = 0; i < active.size(); i++){
            if (active.get(i).getValue().getEnd() > entry.getValue().getEnd()){
                break;
            }
        }
        active.add(i,entry);
    }

    private void SpillAtInterval(Map.Entry<String,Range> entry) {
//        if (active.isEmpty()){
//            registerAllocation.put(entry.getKey(), "spill");
//        }
        Map.Entry<String,Range> spill = active.getLast();
        if (entry.getValue().getEnd() > spill.getValue().getEnd()){
            // spill the entry
            registerAllocation.put(entry.getKey(), "spill");
        } else if(entry.getValue().getEnd() == spill.getValue().getEnd()){
            if (entry.getKey().compareTo(spill.getKey()) < 0){
                registerAllocation.put(entry.getKey(), "spill");
            } else{
                registerAllocation.put(entry.getKey(),registerAllocation.get(spill.getKey()));
                registerAllocation.put(spill.getKey(), "spill");
                active.removeLast();
                active.add(entry);
            }
        }else{
            // spill the last active
            String register = registerAllocation.get(spill.getKey());
            registerAllocation.put(spill.getKey(), "spill");
            registerAllocation.put(entry.getKey(), register);
            active.remove(spill);
            InsertIntoActive(entry);
        }
    }

//    private void ExpireOldIntervals(Map.Entry<String,Range> entry) {
//        for (Map.Entry<String,Range> entry1: active){
//            if (entry1.getValue().getEnd() <= entry.getValue().getStart()){
//                active.remove(entry1);
//                freeReg.add(registerAllocation.get(entry1.getKey()));
//            } else{
//                return;
//            }
//        }
//    }

    private void ExpireOldIntervals(Map.Entry<String,Range> entry) {
        // Use iterator to safely remove elements during iteration
        Iterator<Map.Entry<String, Range>> iterator = active.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Range> activeEntry = iterator.next();
            if (activeEntry.getValue().getEnd() <= entry.getValue().getStart()) {
                // Free the register
                freeReg.add(registerAllocation.get(activeEntry.getKey()));
                // Remove from active list using iterator
                iterator.remove();
            } else {
                // Since active is sorted by end time, we can break early
                break;
            }
        }
    }
}
