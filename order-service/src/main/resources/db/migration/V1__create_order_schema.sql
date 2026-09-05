CREATE SEQUENCE order_code_seq START WITH 1000;
CREATE SEQUENCE return_code_seq START WITH 1000;

CREATE TABLE carriers (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL,
    in_network BOOLEAN NOT NULL DEFAULT TRUE,
    address_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE payment_methods (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    type VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE carts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    discount_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE cart_items (
    id UUID PRIMARY KEY,
    cart_id UUID NOT NULL REFERENCES carts(id),
    product_variant_id UUID NOT NULL,
    quantity INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_cart_items_cart_variant UNIQUE (cart_id, product_variant_id)
);

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    order_code VARCHAR(20) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    discount_id UUID,
    discount_value NUMERIC(12,2),
    subtotal_price NUMERIC(12,2) NOT NULL,
    shipping_fee NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_price NUMERIC(12,2) NOT NULL,
    payment_method_id UUID REFERENCES payment_methods(id),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    payment_last4 VARCHAR(4),
    carrier_id UUID REFERENCES carriers(id),
    tracking_number VARCHAR(50) UNIQUE,
    estimated_delivery_date TIMESTAMPTZ,
    sender_address_id UUID,
    sender_address VARCHAR(500),
    recipient_address_id UUID,
    recipient_address VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    product_variant_id UUID NOT NULL,
    quantity INT NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE tracking_logs (
    id UUID PRIMARY KEY,
    user_id UUID,
    order_id UUID NOT NULL REFERENCES orders(id),
    status VARCHAR(20) NOT NULL,
    title VARCHAR(255),
    location VARCHAR(255),
    latitude NUMERIC(10,7),
    longitude NUMERIC(10,7),
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE return_requests (
    id UUID PRIMARY KEY,
    return_code VARCHAR(20) NOT NULL UNIQUE,
    order_id UUID NOT NULL REFERENCES orders(id),
    user_id UUID NOT NULL,
    reason VARCHAR(30) NOT NULL,
    reason_note VARCHAR(500),
    origin_type VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_ACTION',
    refund_amount NUMERIC(12,2),
    carrier_id UUID REFERENCES carriers(id),
    tracking_number VARCHAR(50) UNIQUE,
    warehouse_id UUID,
    received_at TIMESTAMPTZ,
    restocked_at TIMESTAMPTZ,
    refunded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE return_request_items (
    id UUID PRIMARY KEY,
    return_request_id UUID NOT NULL REFERENCES return_requests(id),
    order_item_id UUID NOT NULL REFERENCES order_items(id),
    quantity INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);
