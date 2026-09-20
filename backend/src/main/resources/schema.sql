CREATE TABLE IF NOT EXISTS plot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(64),
    area DOUBLE,
    status VARCHAR(16) NOT NULL DEFAULT '在用',
    note VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tree (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL UNIQUE,
    plot_id BIGINT NOT NULL,
    variety VARCHAR(64),
    plant_year INT,
    status VARCHAR(16) NOT NULL DEFAULT '正常',
    note VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS harvest_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plot_id BIGINT NOT NULL,
    batch_date VARCHAR(10) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT '待采',
    estimate_kg DOUBLE,
    actual_kg DOUBLE,
    variety VARCHAR(64),
    -- 乐观版本号：已入仓批次改品种/实际公斤时做 CAS，同一批次并发的两笔调账只许一笔成功
    version BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    variety VARCHAR(64) NOT NULL UNIQUE,
    stock_kg DOUBLE NOT NULL DEFAULT 0,
    warn_line DOUBLE NOT NULL DEFAULT 0,
    updated_at VARCHAR(32)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tree_removal (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tree_id BIGINT NOT NULL,
    reason VARCHAR(255),
    prev_status VARCHAR(16),
    status VARCHAR(16) NOT NULL DEFAULT '有效',
    created_at VARCHAR(32),
    withdrawn_at VARCHAR(32)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS batch_tree (
    batch_id BIGINT NOT NULL,
    tree_id BIGINT NOT NULL,
    PRIMARY KEY (batch_id, tree_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS spray_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plot_id BIGINT NOT NULL,
    pesticide VARCHAR(64) NOT NULL,
    spray_date VARCHAR(10) NOT NULL,
    interval_days INT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT '有效',
    note VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO plot (id, code, name, area, status, note) VALUES
 (1, 'P01', '北坡苹果区', 12.5, '在用', '光照好'),
 (2, 'P02', '南坡梨区', 8.0, '在用', NULL),
 (3, 'P03', '温室大棚', 2.0, '停用', '检修中');

INSERT IGNORE INTO tree (id, code, plot_id, variety, plant_year, status, note) VALUES
 (1, 'T0001', 1, '红富士', 2019, '正常', NULL),
 (2, 'T0002', 1, '红富士', 2019, '病害', '叶斑病'),
 (3, 'T0003', 2, '雪梨', 2020, '正常', NULL),
 (4, 'T0004', 1, '红富士', 2021, '正常', NULL);

INSERT IGNORE INTO harvest_batch (id, plot_id, batch_date, status, estimate_kg, actual_kg, variety) VALUES
 (1, 1, '2026-09-10', '已入仓', 300.0, 280.0, '红富士'),
 (2, 2, '2026-09-12', '采集中', 200.0, NULL, '雪梨');

INSERT IGNORE INTO batch_tree (batch_id, tree_id) VALUES
 (1, 1), (1, 4),
 (2, 3);

INSERT IGNORE INTO inventory (id, variety, stock_kg, warn_line, updated_at) VALUES
 (1, '红富士', 280.0, 100.0, '2026-09-10 18:00:00'),
 (2, '雪梨', 0.0, 80.0, '2026-09-12 09:00:00');

INSERT IGNORE INTO spray_record (id, plot_id, pesticide, spray_date, interval_days, status, note) VALUES
 (1, 1, '氯氰菊酯', '2026-09-18', 7, '有效', '蚜虫露头，补喷一次'),
 (2, 2, '波尔多液', '2026-09-01', 10, '有效', '预防性喷施');
