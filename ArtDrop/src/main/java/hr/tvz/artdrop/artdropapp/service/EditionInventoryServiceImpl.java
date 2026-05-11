package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.OrderStatus;
import hr.tvz.artdrop.artdropapp.model.SaleType;
import hr.tvz.artdrop.artdropapp.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EditionInventoryServiceImpl implements EditionInventoryService {

    private static final List<OrderStatus> COUNTED_STATUSES =
            List.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final OrderRepository orderRepository;

    public EditionInventoryServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Integer remaining(Artwork artwork) {
        if (artwork.getSaleType() != SaleType.EDITION || artwork.getEditionSize() == null) {
            return null;
        }
        long sold = orderRepository.countByArtworkIdAndStatusIn(artwork.getId(), COUNTED_STATUSES);
        return Math.max(0, artwork.getEditionSize() - (int) sold);
    }

    @Override
    public boolean canFulfill(Artwork artwork, int quantity) {
        if (quantity <= 0) return false;
        if (artwork.getSaleType() == SaleType.ORIGINAL) {
            return quantity == 1;
        }
        Integer remaining = remaining(artwork);
        return remaining != null && remaining >= quantity;
    }
}
