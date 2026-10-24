-- Modelo inicial PostgreSQL. Não executado em banco real nesta entrega.
-- Dimensões: milímetros. Pesos: gramas. Valores desconhecidos: NULL.

CREATE TABLE product (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku text NOT NULL UNIQUE,
    description text NOT NULL,
    category text NOT NULL CHECK (category IN (
        'PC', 'COMPONENT', 'PERIPHERAL', 'MONITOR', 'MONITOR_SUPPORT',
        'UPS', 'CHAIR', 'DESK', 'DESK_LEG', 'OTHER'
    )),
    is_oem boolean NOT NULL DEFAULT false
);

CREATE TABLE packaging_profile (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id bigint NOT NULL REFERENCES product(id),
    revision integer NOT NULL CHECK (revision > 0),
    role text NOT NULL CHECK (role IN (
        'PREPARED_PC', 'PACKAGED_ITEM', 'ACCOMPANYING_BOX', 'FACTORY_PACKAGE'
    )),
    package_code text NOT NULL,
    packages_per_unit integer NOT NULL DEFAULT 1 CHECK (packages_per_unit > 0),
    length_mm integer CHECK (length_mm > 0),
    width_mm integer CHECK (width_mm > 0),
    height_mm integer CHECK (height_mm > 0),
    actual_weight_g integer CHECK (actual_weight_g >= 0),
    -- Rotations and support constraints must be interpreted by the packing engine.
    orientation_rules jsonb,
    stacking_rules jsonb,
    data_status text NOT NULL DEFAULT 'PENDING'
        CHECK (data_status IN ('PENDING', 'DEMO', 'VALIDATED')),
    source text,
    measured_by text,
    measured_at timestamptz,
    UNIQUE (product_id, revision, role, package_code),
    CHECK (data_status <> 'VALIDATED' OR (
        length_mm IS NOT NULL AND width_mm IS NOT NULL AND height_mm IS NOT NULL
        AND actual_weight_g IS NOT NULL AND orientation_rules IS NOT NULL
        AND stacking_rules IS NOT NULL AND source IS NOT NULL
    ))
);

CREATE TABLE shipping_box (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code text NOT NULL,
    revision integer NOT NULL CHECK (revision > 0),
    inner_length_mm integer CHECK (inner_length_mm > 0),
    inner_width_mm integer CHECK (inner_width_mm > 0),
    inner_height_mm integer CHECK (inner_height_mm > 0),
    outer_length_mm integer CHECK (outer_length_mm > 0),
    outer_width_mm integer CHECK (outer_width_mm > 0),
    outer_height_mm integer CHECK (outer_height_mm > 0),
    tare_weight_g integer CHECK (tare_weight_g >= 0),
    max_content_weight_g integer CHECK (max_content_weight_g > 0),
    cost numeric(12,2) CHECK (cost >= 0),
    available boolean NOT NULL DEFAULT false,
    data_status text NOT NULL DEFAULT 'PENDING'
        CHECK (data_status IN ('PENDING', 'DEMO', 'VALIDATED')),
    UNIQUE (code, revision),
    CHECK (data_status <> 'VALIDATED' OR (
        inner_length_mm IS NOT NULL AND inner_width_mm IS NOT NULL
        AND inner_height_mm IS NOT NULL AND outer_length_mm IS NOT NULL
        AND outer_width_mm IS NOT NULL AND outer_height_mm IS NOT NULL
        AND tare_weight_g IS NOT NULL AND max_content_weight_g IS NOT NULL
    )),
    CHECK (outer_length_mm >= inner_length_mm),
    CHECK (outer_width_mm >= inner_width_mm),
    CHECK (outer_height_mm >= inner_height_mm)
);

CREATE TABLE sales_order (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    external_reference text NOT NULL UNIQUE,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- One row per purchased PC unit, even when two PCs share a commercial SKU.
CREATE TABLE pc_configuration (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id bigint NOT NULL REFERENCES sales_order(id),
    commercial_product_id bigint NOT NULL REFERENCES product(id),
    external_instance_reference text NOT NULL,
    final_composition_revision text NOT NULL,
    UNIQUE (order_id, external_instance_reference),
    UNIQUE (id, order_id)
);

-- Records final purchased composition, not discarded alternatives.
CREATE TABLE order_item (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id bigint NOT NULL REFERENCES sales_order(id),
    configuration_id bigint,
    external_line_reference text NOT NULL,
    product_id bigint NOT NULL REFERENCES product(id),
    quantity integer NOT NULL CHECK (quantity > 0),
    origin text NOT NULL CHECK (origin IN ('BASE', 'CUSTOMIZATION', 'STANDALONE')),
    UNIQUE (order_id, external_line_reference),
    FOREIGN KEY (configuration_id, order_id) REFERENCES pc_configuration(id, order_id),
    CHECK ((origin = 'STANDALONE' AND configuration_id IS NULL)
        OR (origin IN ('BASE', 'CUSTOMIZATION') AND configuration_id IS NOT NULL))
);

CREATE TABLE packing_group (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id bigint NOT NULL REFERENCES sales_order(id),
    configuration_id bigint,
    kind text NOT NULL CHECK (kind IN ('PC_MAIN', 'STANDALONE_SHARED', 'FACTORY_DIRECT')),
    reserve_ratio numeric(5,4) NOT NULL DEFAULT 0.15,
    status text NOT NULL DEFAULT 'PENDING_DATA'
        CHECK (status IN ('PENDING_DATA', 'READY_TO_SIMULATE', 'REVIEW_REQUIRED', 'VALIDATED')),
    FOREIGN KEY (configuration_id, order_id) REFERENCES pc_configuration(id, order_id),
    CHECK ((kind = 'PC_MAIN' AND configuration_id IS NOT NULL)
        OR (kind = 'STANDALONE_SHARED' AND configuration_id IS NULL)
        OR kind = 'FACTORY_DIRECT'),
    CHECK ((kind = 'FACTORY_DIRECT' AND reserve_ratio = 0)
        OR (kind <> 'FACTORY_DIRECT' AND reserve_ratio = 0.15))
);

CREATE UNIQUE INDEX one_main_group_per_pc
    ON packing_group(configuration_id) WHERE kind = 'PC_MAIN';
CREATE UNIQUE INDEX one_shared_group_per_order
    ON packing_group(order_id) WHERE kind = 'STANDALONE_SHARED';

-- Next implementation: preparation of physical objects, containment links,
-- assignment to groups, packing runs, placements and shipped package records.
-- Group status must only be promoted after those validations are implemented.
-- Schema constraints alone do not prove geometric fit or operational readiness.
