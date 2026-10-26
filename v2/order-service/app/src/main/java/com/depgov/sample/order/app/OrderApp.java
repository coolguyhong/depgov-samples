// SMP-C01: StringUtils.isBlank 정적 호출; SMP-C07: StringKit.pad/Money.roundHalfUp 정적 호출.
package com.depgov.sample.order.app;

import com.depgov.common.util.Money;
import com.depgov.common.util.StringKit;
import com.depgov.sample.order.app.SummaryWriter.OrderSummary;
import com.depgov.sample.order.app.SummaryWriter.Summary;
import com.depgov.sample.order.domain.Order;
import com.depgov.sample.order.domain.OrderCalculator;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public final class OrderApp {
    private OrderApp() {
    }

    public static void main(String[] args) throws Exception {
        if ((args.length != 2 && args.length != 4)
                || StringUtils.isBlank(args[0]) || StringUtils.isBlank(args[1])
                || (args.length == 4 && (!"--partner-url".equals(args[2]) || StringUtils.isBlank(args[3])))) {
            throw new IllegalArgumentException(
                    "Usage: OrderApp <input.json> <output.json> [--partner-url <baseUrl>]");
        }
        String partnerUrl = args.length == 4 ? args[3] : null;
        List<Order> orders = new OrderReader().read(Path.of(args[0]));
        OrderCalculator calculator = new OrderCalculator();
        PartnerLookup partners = new PartnerLookup();
        List<OrderSummary> results = new ArrayList<>();
        long subtotal = 0;
        long tax = 0;
        long total = 0;
        for (Order order : orders) {
            long orderSubtotal = calculator.subtotal(order);
            long orderTax = Money.roundHalfUp(orderSubtotal, 10);
            long orderTotal = Math.addExact(orderSubtotal, orderTax);
            results.add(new OrderSummary(order.orderId(), StringKit.pad(order.customerId(), 8),
                    orderSubtotal, orderTax, orderTotal, AttachmentNames.extensions(order.attachments()),
                    partners.lookup(partnerUrl, order.customerId())));
            subtotal = Math.addExact(subtotal, orderSubtotal);
            tax = Math.addExact(tax, orderTax);
            total = Math.addExact(total, orderTotal);
        }
        Summary summary = new Summary(results.size(), subtotal, tax, total, List.copyOf(results));
        SummaryWriter writer = new SummaryWriter();
        writer.write(Path.of(args[1]), summary);
        System.out.println(writer.streamingLine(summary));
    }
}
