package io.draft2done.app;

public final class RequestRegistryTest {
    private static int checks;
    private static void expect(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    public static void main(String[] args){
        RequestRegistry registry=new RequestRegistry();
        registry.enqueue("queued");
        expect(registry.cancel("queued")&&registry.isCancelled("queued"),"Cancellation survives until queued worker checks it");
        expect(!registry.cancel("unrelated"),"Unknown request does not retain cancellation state");
        registry.finish("queued");
        expect(!registry.isCancelled("queued")&&registry.activeExcept("").isEmpty(),"Completion clears request state");
        registry.enqueue("queued");
        expect(!registry.isCancelled("queued"),"A completed ID starts clean");
        boolean duplicate=false;try{registry.enqueue("queued");}catch(IllegalStateException expected){duplicate=true;}
        expect(duplicate,"Duplicate active request IDs rejected");
        registry.enqueue("other");
        expect(registry.activeExcept("queued").size()==1&&registry.activeExcept("queued").contains("other"),"Account changes can cancel other requests without cancelling themselves");
        for(int i=0;i<10;i++)registry.enqueue("limit-"+i);
        boolean limited=false;try{registry.enqueue("overflow");}catch(IllegalStateException expected){limited=true;}
        expect(limited,"Active requests are bounded");
        System.out.println("Request lifecycle checks passed: "+checks);
    }
}
