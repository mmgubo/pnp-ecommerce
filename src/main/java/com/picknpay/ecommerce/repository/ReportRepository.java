package com.picknpay.ecommerce.repository;

import com.picknpay.ecommerce.dto.response.OrderSummaryResponse;
import com.picknpay.ecommerce.dto.response.TopProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Reporting queries use NamedParameterJdbcTemplate rather than JPA so the JOIN /
 * GROUP BY / SUM / COUNT(DISTINCT) shape stays explicit and reviewable.
 */
@Repository
@RequiredArgsConstructor
public class ReportRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String TOP_PRODUCTS_SQL = """
            SELECT  p.id                                AS product_id,
                    p.sku                               AS product_sku,
                    p.name                              AS product_name,
                    SUM(oi.quantity)                    AS total_units_sold,
                    COUNT(DISTINCT o.id)                AS order_count,
                    SUM(CAST(oi.quantity AS BIGINT) * oi.unit_price) AS total_revenue
            FROM    order_items oi
            JOIN    orders   o ON o.id = oi.order_id
            JOIN    products p ON p.id = oi.product_id
            WHERE   o.status = 'CONFIRMED'
              AND   (:fromTs IS NULL OR o.created_at >= :fromTs)
              AND   (:toTs   IS NULL OR o.created_at <  :toTs)
            GROUP BY p.id, p.sku, p.name
            ORDER BY total_units_sold DESC, p.name ASC
            LIMIT :limit
            """;

    private static final String ORDER_SUMMARY_SQL = """
            SELECT  status,
                    COUNT(*)                                       AS order_count,
                    COALESCE(SUM(total_amount), 0)                 AS total_revenue,
                    COALESCE(CAST(AVG(total_amount) AS BIGINT), 0) AS average_order_value
            FROM    orders
            GROUP BY status
            ORDER BY status
            """;

    public List<TopProductResponse> topProducts(int limit, LocalDate from, LocalDate to) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit",  limit)
                // Treat 'from' as start-of-day and 'to' as end-of-day inclusive by using
                // the exclusive upper bound (to + 1 day at 00:00).
                .addValue("fromTs", from == null ? null : from.atStartOfDay())
                .addValue("toTs",   to   == null ? null : to.plusDays(1).atStartOfDay());

        return jdbc.query(TOP_PRODUCTS_SQL, params, (rs, n) -> TopProductResponse.of(
                rs.getLong("product_id"),
                rs.getString("product_sku"),
                rs.getString("product_name"),
                rs.getLong("total_units_sold"),
                rs.getLong("order_count"),
                rs.getLong("total_revenue")
        ));
    }

    public List<OrderSummaryResponse> orderSummary() {
        return jdbc.query(ORDER_SUMMARY_SQL, (rs, n) -> OrderSummaryResponse.of(
                rs.getString("status"),
                rs.getLong("order_count"),
                rs.getLong("total_revenue"),
                rs.getLong("average_order_value")
        ));
    }
}
