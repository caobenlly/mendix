# Line Chart: Số người tham gia dự án theo Phòng Ban & Ngày

---

## Mô tả bài toán

| Thành phần | Mô tả |
|:---|:---|
| **Trục X** | Ngày (timeline theo ngày/tháng) |
| **Trục Y** | Số người tham gia dự án |
| **Đường line** | Mỗi phòng ban = 1 đường line riêng |
| **Tooltip khi hover** | Tên phòng + Số người tham gia tại ngày đó |
| **Nguồn dữ liệu** | Elasticsearch Index `project_analysis` |

---

## Tổng quan luồng xử lý

```
[Elasticsearch Index: project_analysis]
        ↓  (POST /_search + Aggregation query)
[Mendix: Call REST (POST)]
        ↓  (Apply Import Mapping)
[Mendix Entity: LineChartData]
        ↓  (Microflow: BuildLineChartJson)
[Chuỗi JSON chuẩn cho Chart]
        ↓
[Page: LineChartPage]
        ↓
[HTML Snippet / Chart Widget]
        ↓
[Chart.js / ApexCharts] → Render Line Chart + Tooltip
```

---

## PHẦN 1: Elasticsearch - Cấu trúc dữ liệu

### 1.1 - Tạo Index

```bash
curl -X PUT "http://localhost:9200/project_analysis" \
  -H "Content-Type: application/json" \
  -d '{
    "mappings": {
      "properties": {
        "allocation_date":     { "type": "date",    "format": "yyyy-MM-dd" },
        "department_id":       { "type": "integer" },
        "department_name":     { "type": "keyword" },
        "user_id":             { "type": "keyword" },
        "project_id":          { "type": "keyword" },
        "project_name":        { "type": "keyword" },
        "hours_planned":       { "type": "float" }
      }
    }
  }'
```

---

### 1.2 - Bulk Import dữ liệu mẫu

```bash
curl -X POST "http://localhost:9200/project_analysis/_bulk" \
  -H "Content-Type: application/json" \
  -d '
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-05", "department_id": 10, "department_name": "Phòng Phát Triển Công Nghệ 1", "user_id": "U001", "project_id": "P101", "project_name": "Dự án Alpha", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-05", "department_id": 10, "department_name": "Phòng Phát Triển Công Nghệ 1", "user_id": "U002", "project_id": "P101", "project_name": "Dự án Alpha", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-05", "department_id": 10, "department_name": "Phòng Phát Triển Công Nghệ 1", "user_id": "U003", "project_id": "P102", "project_name": "Dự án Beta", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-05", "department_id": 20, "department_name": "Phòng Kinh Doanh", "user_id": "U010", "project_id": "P101", "project_name": "Dự án Alpha", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-05", "department_id": 20, "department_name": "Phòng Kinh Doanh", "user_id": "U011", "project_id": "P103", "project_name": "Dự án Gamma", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-05", "department_id": 30, "department_name": "Phòng QA", "user_id": "U020", "project_id": "P102", "project_name": "Dự án Beta", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-12", "department_id": 10, "department_name": "Phòng Phát Triển Công Nghệ 1", "user_id": "U001", "project_id": "P101", "project_name": "Dự án Alpha", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-12", "department_id": 10, "department_name": "Phòng Phát Triển Công Nghệ 1", "user_id": "U004", "project_id": "P102", "project_name": "Dự án Beta", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-12", "department_id": 20, "department_name": "Phòng Kinh Doanh", "user_id": "U010", "project_id": "P101", "project_name": "Dự án Alpha", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-12", "department_id": 20, "department_name": "Phòng Kinh Doanh", "user_id": "U012", "project_id": "P101", "project_name": "Dự án Alpha", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-12", "department_id": 20, "department_name": "Phòng Kinh Doanh", "user_id": "U013", "project_id": "P103", "project_name": "Dự án Gamma", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-12", "department_id": 30, "department_name": "Phòng QA", "user_id": "U020", "project_id": "P102", "project_name": "Dự án Beta", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-12", "department_id": 30, "department_name": "Phòng QA", "user_id": "U021", "project_id": "P102", "project_name": "Dự án Beta", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-19", "department_id": 10, "department_name": "Phòng Phát Triển Công Nghệ 1", "user_id": "U001", "project_id": "P101", "project_name": "Dự án Alpha", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-19", "department_id": 10, "department_name": "Phòng Phát Triển Công Nghệ 1", "user_id": "U002", "project_id": "P101", "project_name": "Dự án Alpha", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-19", "department_id": 10, "department_name": "Phòng Phát Triển Công Nghệ 1", "user_id": "U005", "project_id": "P104", "project_name": "Dự án Delta", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-19", "department_id": 20, "department_name": "Phòng Kinh Doanh", "user_id": "U010", "project_id": "P103", "project_name": "Dự án Gamma", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-19", "department_id": 30, "department_name": "Phòng QA", "user_id": "U020", "project_id": "P104", "project_name": "Dự án Delta", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-19", "department_id": 30, "department_name": "Phòng QA", "user_id": "U022", "project_id": "P104", "project_name": "Dự án Delta", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-19", "department_id": 30, "department_name": "Phòng QA", "user_id": "U023", "project_id": "P102", "project_name": "Dự án Beta", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-26", "department_id": 10, "department_name": "Phòng Phát Triển Công Nghệ 1", "user_id": "U001", "project_id": "P104", "project_name": "Dự án Delta", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-26", "department_id": 10, "department_name": "Phòng Phát Triển Công Nghệ 1", "user_id": "U002", "project_id": "P104", "project_name": "Dự án Delta", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-26", "department_id": 10, "department_name": "Phòng Phát Triển Công Nghệ 1", "user_id": "U003", "project_id": "P104", "project_name": "Dự án Delta", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-26", "department_id": 10, "department_name": "Phòng Phát Triển Công Nghệ 1", "user_id": "U006", "project_id": "P101", "project_name": "Dự án Alpha", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-26", "department_id": 20, "department_name": "Phòng Kinh Doanh", "user_id": "U014", "project_id": "P103", "project_name": "Dự án Gamma", "hours_planned": 8 }
{ "index": { "_index": "project_analysis" } }
{ "allocation_date": "2026-01-26", "department_id": 30, "department_name": "Phòng QA", "user_id": "U020", "project_id": "P104", "project_name": "Dự án Delta", "hours_planned": 8 }
'
```

---

### 1.3 - Query Aggregation để lấy dữ liệu Line Chart

Câu truy vấn này sẽ group theo **Ngày** (date_histogram) → trong mỗi ngày group tiếp theo **Phòng ban** (terms) → đếm số user duy nhất (`cardinality` trên `user_id`):

```bash
curl -X POST "http://localhost:9200/project_analysis/_search" \
  -H "Content-Type: application/json" \
  -d '{
    "size": 0,
    "aggs": {
      "Theo_Ngay": {
        "date_histogram": {
          "field": "allocation_date",
          "calendar_interval": "week",
          "format": "yyyy-MM-dd",
          "min_doc_count": 0
        },
        "aggs": {
          "Theo_Phong": {
            "terms": {
              "field": "department_name",
              "size": 20
            },
            "aggs": {
              "So_Nguoi": {
                "cardinality": {
                  "field": "user_id"
                }
              }
            }
          }
        }
      }
    }
  }'
```

> **Giải thích:**
> - `date_histogram` → Nhóm theo tuần (`week`). Đổi thành `day` hoặc `month` tùy nhu cầu.
> - `terms` → Nhóm theo tên phòng ban.
> - `cardinality` → Đếm số `user_id` **duy nhất** (tránh đếm trùng người tham gia nhiều dự án trong cùng 1 ngày).

---

### Kết quả trả về từ Elasticsearch:

```json
{
  "aggregations": {
    "Theo_Ngay": {
      "buckets": [
        {
          "key_as_string": "2026-01-05",
          "key": 1767571200000,
          "doc_count": 6,
          "Theo_Phong": {
            "buckets": [
              {
                "key": "Phòng Phát Triển Công Nghệ 1",
                "doc_count": 3,
                "So_Nguoi": { "value": 3 }
              },
              {
                "key": "Phòng Kinh Doanh",
                "doc_count": 2,
                "So_Nguoi": { "value": 2 }
              },
              {
                "key": "Phòng QA",
                "doc_count": 1,
                "So_Nguoi": { "value": 1 }
              }
            ]
          }
        },
        {
          "key_as_string": "2026-01-12",
          "key": 1768176000000,
          "doc_count": 7,
          "Theo_Phong": {
            "buckets": [
              {
                "key": "Phòng Kinh Doanh",
                "doc_count": 3,
                "So_Nguoi": { "value": 3 }
              },
              {
                "key": "Phòng Phát Triển Công Nghệ 1",
                "doc_count": 2,
                "So_Nguoi": { "value": 2 }
              },
              {
                "key": "Phòng QA",
                "doc_count": 2,
                "So_Nguoi": { "value": 2 }
              }
            ]
          }
        }
      ]
    }
  }
}
```

---

## PHẦN 2: Mendix - Domain Model

### 2.1 - Tạo các Entity (non-persistent)

Vì đây là dữ liệu hiển thị tạm thời (không lưu vào DB Mendix), tất cả entity nên đặt **Persistable = No**.

#### Entity 1: `LineChartRoot`
> Đối tượng gốc bao bọc toàn bộ response (map với `aggregations`)

*(Không có thuộc tính, chỉ là container)*

#### Entity 2: `DayBucket`
> Mỗi object đại diện cho một mốc ngày trên trục X

| Attribute | Type | Ghi chú |
|:---|:---|:---|
| `NgayStr` | String | Ngày định dạng "yyyy-MM-dd" (key_as_string) |
| `NgayMs` | Long | Timestamp milliseconds (key) |
| `DocCount` | Integer | Tổng số bản ghi trong ngày đó |

#### Entity 3: `PhongBucket`
> Mỗi object đại diện cho 1 phòng ban trong 1 ngày cụ thể

| Attribute | Type | Ghi chú |
|:---|:---|:---|
| `TenPhong` | String | Tên phòng ban (key) |
| `DocCount` | Integer | Số bản ghi |
| `SoNguoi` | Integer | Số người duy nhất (So_Nguoi.value) |

#### Associations:
```
LineChartRoot  1 ──── *  DayBucket
DayBucket      1 ──── *  PhongBucket
```

---

## PHẦN 3: Mendix - Import Mapping

### 3.1 - JSON Structure mẫu

Tạo **JSON Structure** tên `JSON_LineChart` và paste JSON sau:

```json
{
  "aggregations": {
    "Theo_Ngay": {
      "buckets": [
        {
          "key_as_string": "2026-01-05",
          "key": 1767571200000,
          "doc_count": 6,
          "Theo_Phong": {
            "buckets": [
              {
                "key": "Phòng Phát Triển Công Nghệ 1",
                "doc_count": 3,
                "So_Nguoi": {
                  "value": 3
                }
              }
            ]
          }
        }
      ]
    }
  }
}
```

### 3.2 - Import Mapping `Import_LineChart`

| Node JSON | Map với Entity | Attribute |
|:---|:---|:---|
| `aggregations` (root) | `LineChartRoot` | - |
| `buckets[*]` (ngày) | `DayBucket` | - |
| `key_as_string` | `DayBucket` | `NgayStr` |
| `key` | `DayBucket` | `NgayMs` |
| `doc_count` (ngày) | `DayBucket` | `DocCount` |
| `buckets[*]` (phòng) | `PhongBucket` | - |
| `key` | `PhongBucket` | `TenPhong` |
| `doc_count` (phòng) | `PhongBucket` | `DocCount` |
| `value` (So_Nguoi) | `PhongBucket` | `SoNguoi` |

---

## PHẦN 4: Mendix - Microflow `GetLineChartData`

### Sơ đồ:

```
[Start]
   ↓
[1. Call REST POST → Elasticsearch]  (Apply Import Mapping → LineChartRoot)
   ↓
[2. Retrieve DayBucketList] (via Association LineChartRoot → DayBucket)
   ↓
[3. Retrieve tất cả PhongBucket để lấy danh sách tên phòng duy nhất]
   ↓
[4. Build JSON cho Chart.js / ApexCharts]
   ↓
[5. Return: String ChartJson]
[End]
```

---

### Chi tiết Bước 1 - Call REST:

| Thiết lập | Giá trị |
|:---|:---|
| **Method** | POST |
| **URL** | `http://localhost:9200/project_analysis/_search` |
| **HTTP Headers** | `Content-Type: application/json` |
| **Request Body (Custom string)** | Xem bên dưới |
| **Response handling** | Apply import mapping → `Import_LineChart` |
| **Output variable** | `ChartRoot` (type: `LineChartRoot`) |

**Request Body Template:**
```json
{
  "size": 0,
  "aggs": {
    "Theo_Ngay": {
      "date_histogram": {
        "field": "allocation_date",
        "calendar_interval": "week",
        "format": "yyyy-MM-dd",
        "min_doc_count": 0
      },
      "aggs": {
        "Theo_Phong": {
          "terms": {
            "field": "department_name",
            "size": 20
          },
          "aggs": {
            "So_Nguoi": {
              "cardinality": {
                "field": "user_id"
              }
            }
          }
        }
      }
    }
  }
}
```

---

### Chi tiết Bước 4 - Build JSON Chart:

Cấu trúc JSON chuẩn cho **ApexCharts / Chart.js** dạng Multi-line:

```json
{
  "categories": ["2026-01-05", "2026-01-12", "2026-01-19", "2026-01-26"],
  "series": [
    {
      "name": "Phòng Phát Triển Công Nghệ 1",
      "data": [3, 2, 3, 4]
    },
    {
      "name": "Phòng Kinh Doanh",
      "data": [2, 3, 1, 1]
    },
    {
      "name": "Phòng QA",
      "data": [1, 2, 3, 1]
    }
  ]
}
```

**Logic trong Microflow:**

1. **Retrieve DayBucketList** (sort `NgayStr` ASC) → build mảng `categories`.
2. **Lấy danh sách tên phòng duy nhất** → duyệt qua tất cả `PhongBucket`, tập hợp `TenPhong` không trùng.
3. **Với mỗi phòng ban:** duyệt qua từng `DayBucket` → tìm `PhongBucket` tương ứng → lấy `SoNguoi` (nếu không có → gán `0`).
4. **Ghép thành chuỗi JSON** như cấu trúc trên.

---

## PHẦN 5: Mendix - Page & Widget

### 5.1 - Tạo Page `LineChartPage`

- **Layout:** Atlas_Default
- **Tiêu đề:** "Số người tham gia dự án theo Phòng Ban"

### 5.2 - Cấu hình Data View

1. Kéo **Data View** vào trang.
2. **Data Source:** Microflow → `GetLineChartData`.
3. **Entity:** `LineChartResponse` (non-persistent, chứa `JsonData: String`).

### 5.3 - Thêm thư viện ApexCharts vào index.html

Mở `theme/web/index.html`, thêm vào `<head>`:

```html
<script src="https://cdn.jsdelivr.net/npm/apexcharts"></script>
```

### 5.4 - HTML Snippet Widget

```html
<!-- Container biểu đồ -->
<div id="lineChartContainer"></div>

<!-- Input ẩn chứa JSON từ Mendix -->
<input type="hidden" id="chartJsonInput" value="{1}" />

<script>
(function() {
  var raw = document.getElementById('chartJsonInput').value;
  var chartData;
  try { chartData = JSON.parse(raw); } catch(e) { return; }

  var options = {
    chart: {
      type: 'line',
      height: 420,
      toolbar: { show: true },
      zoom: { enabled: true },
      animations: { enabled: true, speed: 600 }
    },
    stroke: {
      curve: 'smooth',
      width: 3
    },
    markers: {
      size: 6,
      hover: { size: 9 }
    },
    // ✅ Tooltip khi di chuột: hiện tên phòng + số người
    tooltip: {
      shared: true,
      intersect: false,
      y: {
        formatter: function(val, opts) {
          return opts.w.globals.seriesNames[opts.seriesIndex]
                 + ': <b>' + val + ' người</b>';
        }
      },
      x: {
        formatter: function(val) {
          return 'Ngày: ' + val;
        }
      }
    },
    xaxis: {
      categories: chartData.categories,
      title: { text: 'Ngày' },
      labels: { rotate: -45 }
    },
    yaxis: {
      title: { text: 'Số người tham gia' },
      min: 0,
      tickAmount: 5
    },
    legend: {
      position: 'top',
      horizontalAlign: 'center'
    },
    colors: ['#5c6bc0','#42a5f5','#66bb6a','#ff7043','#ab47bc','#ffd54f'],
    series: chartData.series,
    title: {
      text: 'Số người tham gia dự án theo Phòng Ban',
      align: 'center',
      style: { fontSize: '16px', fontWeight: 'bold' }
    }
  };

  var chart = new ApexCharts(
    document.getElementById('lineChartContainer'),
    options
  );
  chart.render();
})();
</script>
```

> **Lưu ý `{1}`:** Đây là placeholder trong Mendix. Bạn cần truyền `$ChartResponse/JsonData` vào tham số `{1}` trong phần **Parameters** của HTML Snippet.

---

## PHẦN 6: Kiểm tra kết quả Elasticsearch

### Kiểm tra index đã có data chưa:
```bash
curl -X GET "http://localhost:9200/project_analysis/_count"
```

### Xem 3 bản ghi đầu:
```bash
curl -X GET "http://localhost:9200/project_analysis/_search?size=3&pretty"
```

### Xem danh sách phòng ban có trong data:
```bash
curl -X POST "http://localhost:9200/project_analysis/_search?pretty" \
  -H "Content-Type: application/json" \
  -d '{
    "size": 0,
    "aggs": {
      "Danh_Sach_Phong": {
        "terms": { "field": "department_name", "size": 50 }
      }
    }
  }'
```

### Kiểm tra toàn bộ query Line Chart:
```bash
curl -X POST "http://localhost:9200/project_analysis/_search?pretty" \
  -H "Content-Type: application/json" \
  -d '{
    "size": 0,
    "aggs": {
      "Theo_Ngay": {
        "date_histogram": {
          "field": "allocation_date",
          "calendar_interval": "week",
          "format": "yyyy-MM-dd",
          "min_doc_count": 0
        },
        "aggs": {
          "Theo_Phong": {
            "terms": { "field": "department_name", "size": 20 },
            "aggs": {
              "So_Nguoi": { "cardinality": { "field": "user_id" } }
            }
          }
        }
      }
    }
  }'
```

---

## PHẦN 7: JSON mẫu cuối cùng truyền vào Chart

Đây là chuỗi JSON mà `GetLineChartData` sẽ build và trả về:

```json
{
  "categories": [
    "2026-01-05",
    "2026-01-12",
    "2026-01-19",
    "2026-01-26"
  ],
  "series": [
    {
      "name": "Phòng Phát Triển Công Nghệ 1",
      "data": [3, 2, 3, 4]
    },
    {
      "name": "Phòng Kinh Doanh",
      "data": [2, 3, 1, 1]
    },
    {
      "name": "Phòng QA",
      "data": [1, 2, 3, 1]
    }
  ]
}
```

---

## Tóm tắt toàn bộ luồng

```
① Elasticsearch Bulk Import (curl) → Index: project_analysis
        ↓
② Elasticsearch Aggregation Query (curl kiểm tra)
        ↓
③ Mendix Domain Model:
   LineChartRoot → DayBucket → PhongBucket
        ↓
④ JSON Structure + Import Mapping (Import_LineChart)
        ↓
⑤ Microflow: GetLineChartData
   ├── Call REST POST (Elasticsearch Agg Query)
   ├── Apply Import Mapping → LineChartRoot
   ├── Build JSON: categories[] + series[{name, data[]}]
   └── Return String: ChartJson
        ↓
⑥ Page: LineChartPage
   └── Data View (source: GetLineChartData)
         └── HTML Snippet (ApexCharts)
               ├── Nhận ChartJson qua {1}
               └── Render Line Chart + Tooltip hover
```
