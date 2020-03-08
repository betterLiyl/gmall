package com.gmall.order.task;

import com.alibaba.dubbo.config.annotation.Reference;
import com.gmall.bean.OrderInfo;
import com.gmall.enums.ProcessStatus;
import com.gmall.service.OrderService;
import com.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@EnableScheduling
public class OrderTask {

    @Reference
    OrderService orderService;
    @Reference
    PaymentService paymentService;
    @Scheduled(cron = "0/5 * * * * ?")
    public void work() throws InterruptedException {
        System.out.println("thread = ===============" + Thread.currentThread());
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(5);
        return taskScheduler;
    }


    @Scheduled(cron = "0/30 * * * * ?")
    public void checkUnpaidOrder() {
        System.out.println("开始检查未付款单据 = ");
        Long beginTime=System.currentTimeMillis();
        List<OrderInfo> unpaidOrderList = orderService.getUnpaidOrderList();
        for (OrderInfo orderInfo : unpaidOrderList) {
           checkExpireOrder(orderInfo);
        }
        Long costtime=System.currentTimeMillis()-beginTime;
        System.out.println("开始检查完毕未付款单据 = 共消耗"+costtime);
    }

    @Async
    public  void  checkExpireOrder(OrderInfo orderInfo  )   {

        updateProcessStatus(orderInfo.getId(), ProcessStatus.CLOSED);
        paymentService.closePayment(orderInfo.getId());


        return ;

    }

    private void updateProcessStatus(String id, ProcessStatus closed) {
        orderService.updateOrderStatus(id,closed);
    }
}
