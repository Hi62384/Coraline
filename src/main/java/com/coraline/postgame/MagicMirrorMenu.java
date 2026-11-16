package com.coraline.postgame;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Selection is a ghost: the original never leaves the real player inventory.
 * Each quote authorizes exactly one confirmation of that precise stack. */
public final class MagicMirrorMenu extends AbstractContainerMenu {
    private final MagicMirrorBlockEntity mirror;
    private final Inventory inventory;
    private final SimpleContainer preview=new SimpleContainer(1);
    private final ContainerData data;
    private final int[] words=new int[15];
    private int selected=-1,quantity=1;
    private long feedbackUntil;
    private UUID nonce=UUID.randomUUID();
    private ItemStack snapshot=ItemStack.EMPTY;
    private MirrorPricing.Price quote=new MirrorPricing.Price(MirrorPricing.Problem.EMPTY,0,0);
    // status: 0 ready, 1 select, 2 restricted, 3 unsupported, 4 funds,
    // 5 space, 6 completed, 7 save paused, 8 cooldown, 9 stale quote.
    public MagicMirrorMenu(int id,Inventory inventory,FriendlyByteBuf buffer){this(id,inventory,(MagicMirrorBlockEntity)null);buffer.readBlockPos();}
    public MagicMirrorMenu(int id,Inventory inventory,MagicMirrorBlockEntity mirror){
        super(com.coraline.registry.ModMenus.MAGIC_MIRROR.get(),id);this.inventory=inventory;this.mirror=mirror;
        addSlot(new Slot(preview,0,12,34){@Override public boolean mayPlace(ItemStack stack){return false;}@Override public boolean mayPickup(Player player){return false;}});
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inventory,x+y*9+9,31+x*18,152+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inventory,x,31+x*18,210));
        data=inventory.player.level().isClientSide?new SimpleContainerData(words.length):new ContainerData(){public int get(int i){return words[i];}public void set(int i,int value){}public int getCount(){return words.length;}};addDataSlots(data);
        if(!inventory.player.level().isClientSide)refresh(true);
    }
    public int quantity(){return data.get(0)&65535;}
    public int maximum(){return data.get(1)&65535;}
    public int cost(){return (data.get(2)&65535)|((data.get(3)&65535)<<16);}
    public int status(){return data.get(4)&65535;}
    public boolean ready(){return (data.get(5)&65535)==1;}
    public UUID quoteNonce(){long a=0,b=0;for(int i=0;i<4;i++){a|=(long)(data.get(6+i)&65535)<<(i*16);b|=(long)(data.get(10+i)&65535)<<(i*16);}return new UUID(a,b);}
    public ItemStack outputPreview(){return ready()?preview.getItem(0).copyWithCount(quantity()):ItemStack.EMPTY;}
    private ItemStack input(){return selected>=0&&selected<36?inventory.getItem(selected):ItemStack.EMPTY;}
    private void rotate(){nonce=UUID.randomUUID();for(int i=0;i<4;i++){words[6+i]=(int)((nonce.getMostSignificantBits()>>>(i*16))&65535);words[10+i]=(int)((nonce.getLeastSignificantBits()>>>(i*16))&65535);}}
    private void refresh(boolean changed){
        var p=(ServerPlayer)inventory.player;var stack=input();var eligibility=MirrorPricing.eligibility(p.serverLevel(),stack);
        int max=eligibility==MirrorPricing.Problem.NONE?Math.min(stack.getCount(),Math.min(stack.getMaxStackSize(),MirrorConfig.MAX_QUANTITY.get())):0;
        quantity=Math.max(1,Math.min(quantity,Math.max(1,max)));var price=MirrorPricing.price(p.serverLevel(),stack,quantity);
        if(changed||!ItemStack.matches(stack,snapshot)||!price.equals(quote)){
            snapshot=stack.copy();quote=price;rotate();
            // Never transmit unsupported capability/ownership NBT as a preview.
            preview.setItem(0,stack.isEmpty()?ItemStack.EMPTY:price.valid()?stack.copyWithCount(quantity):new ItemStack(stack.getItem()));
            words[4]=switch(price.problem()){case NONE->0;case EMPTY->1;case RESTRICTED->2;default->3;};
        }
        words[0]=quantity;words[1]=max;words[2]=quote.diamonds()&65535;words[3]=(quote.diamonds()>>>16)&65535;words[5]=quote.valid()?1:0;
        if(quote.valid()&&words[4]!=7&&p.server.overworld().getGameTime()>=feedbackUntil){
            var planned=MirrorWallet.plan(p,snapshot.copyWithCount(quantity),quote.diamonds());words[4]=planned.failure()==MirrorWallet.Failure.FUNDS?4:planned.failure()==MirrorWallet.Failure.SPACE?5:0;
        }
    }
    @Override public void broadcastChanges(){if(!inventory.player.level().isClientSide)refresh(false);super.broadcastChanges();}
    @Override public boolean stillValid(Player player){return player.level().isClientSide||mirror!=null&&mirror.canUse(player);}
    @Override public void clicked(int index,int button,ClickType type,Player player){
        if(!stillValid(player))return;
        if(index==0){
            if(!player.level().isClientSide){
                selected=-1;quantity=1;
                if(!getCarried().isEmpty()&&type==ClickType.PICKUP){
                    // Accept a cursor stack by putting it into a real empty
                    // inventory cell first. Closing never returns the ghost.
                    for(int cell=0;cell<36;cell++)if(inventory.getItem(cell).isEmpty()){inventory.setItem(cell,getCarried());setCarried(ItemStack.EMPTY);selected=cell;inventory.setChanged();break;}
                }
                refresh(true);
            }return;
        }
        if(index>0&&index<slots.size()&&getCarried().isEmpty()&&(type==ClickType.PICKUP||type==ClickType.QUICK_MOVE)){
            if(!player.level().isClientSide){selected=slots.get(index).getContainerSlot();quantity=1;refresh(true);}return;
        }
        // Dragging/pick-all and creative cloning cannot extract the ghost.
        if(type==ClickType.PICKUP_ALL||type==ClickType.QUICK_CRAFT)return;
        super.clicked(index,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index){return ItemStack.EMPTY;}
    @Override public boolean clickMenuButton(Player player,int id){
        if(!(player instanceof ServerPlayer)||!stillValid(player)||id<0||id>65535)return false;refresh(false);if(id>=1025){int wanted=id-1024;if(wanted<1||wanted>words[1])return false;quantity=wanted;}else if(id<=2)quantity=id==0?Math.max(1,quantity-1):id==1?Math.min(words[1],quantity+1):Math.max(1,words[1]);else return false;refresh(true);broadcastChanges();return true;
    }
    void selectQuantity(ServerPlayer player,int wanted){
        if(player!=inventory.player||player.containerMenu!=this||!stillValid(player)||wanted<1)return;
        refresh(false);if(wanted>words[1])return;quantity=wanted;refresh(true);broadcastChanges();
    }
    void confirm(ServerPlayer p,UUID submitted){
        if(p!=inventory.player||p.containerMenu!=this||!getCarried().isEmpty()||!stillValid(p)||!submitted.equals(nonce))return;
        var original=input();var current=MirrorPricing.price(p.serverLevel(),original,quantity);
        if(!ItemStack.matches(original,snapshot)||!current.equals(quote)||!current.valid()){refresh(true);words[4]=9;feedbackUntil=p.server.overworld().getGameTime()+20;broadcastChanges();return;}
        var permission=net.minecraftforge.common.ForgeHooks.onRightClickBlock(p,InteractionHand.MAIN_HAND,mirror.getBlockPos(),new net.minecraft.world.phys.BlockHitResult(mirror.getBlockPos().getCenter(),net.minecraft.core.Direction.UP,mirror.getBlockPos(),false));
        if(permission.isCanceled()||permission.getUseBlock()==net.minecraftforge.eventbus.api.Event.Result.DENY||p.containerMenu!=this||!stillValid(p)||!ItemStack.matches(input(),snapshot)||!MirrorPricing.price(p.serverLevel(),input(),quantity).equals(quote))return;
        var planned=MirrorWallet.plan(p,snapshot.copyWithCount(quantity),quote.diamonds());rotate();
        if(planned.failure()!=null){words[4]=planned.failure()==MirrorWallet.Failure.FUNDS?4:5;broadcastChanges();return;}
        var result=MirrorTransactions.commit(p,planned.plan(),submitted);refresh(true);
        words[4]=switch(result){case COMMITTED->6;case FAILED->9;case FROZEN->7;case COOLDOWN->8;};
        feedbackUntil=p.server.overworld().getGameTime()+20;
        if(result==MirrorTransactions.Result.COMMITTED)mirror.completed();broadcastChanges();
    }
}
