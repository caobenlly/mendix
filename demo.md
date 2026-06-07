# Hướng dẫn hiển thị Gantt Chart từ Oracle DB lên Mendix Frontend

---

## Tổng quan luồng xử lý

```
Oracle DB (GANTTTASK)
        ↓
  [Mendix Domain Model]
        ↓
  [Microflow: GetGanttData]  ← Retrieve từ DB → Build JSON String
        ↓
  [REST Endpoint / Nanoflow]
        ↓
  [Page: GanttPage]
        ↓
  [Custom Widget / HTML Widget] → Render Gantt Chart
```

---

## BƯỚC 1: Domain Model

Tạo Entity **`GanttTask`** trong `MyFirstModule` với các thuộc tính:

| Attribute | Type | Ghi chú |
|:---|:---|:---|
| `ProjectTaskId` | Integer | ID tự quản lý (map với cột PROJECTTASKID) |
| `ParentProjectTaskId` | Integer | ID cha (map với cột PARENTPROJECTTASKID) |
| `ProjectName` | String (200) | Tên công việc |
| `StartDate` | DateTime | Ngày bắt đầu |
| `EndDate` | DateTime | Ngày kết thúc |
| `Duration` | Integer | Số ngày thực hiện |
| `Progress` | Decimal | Tiến độ 0.0 → 1.0 |
| `IsOpen` | Boolean | Có mở rộng cây hay không |
| `Color` | String (200) | Mã màu HEX của thanh task |
| `ProgressColor` | String (200) | Mã màu HEX phần tiến độ |
| `TaskType` | String (200) | "project" / "task" / "milestone" |

> **Lưu ý:** Entity này map 1-1 với bảng `MYFIRSTMODULE$GANTTTASK` trong Oracle.
> Persistable = **Yes** (vì đang lấy data từ Oracle DB có sẵn).

---

## BƯỚC 2: Tạo Microflow `GetGanttJsonData`

Microflow này truy vấn DB và build chuỗi JSON để trả về cho widget.

### Sơ đồ các bước:

```
[Start]
   ↓
[1. Retrieve GanttTaskList from DB]
   ↓
[2. Create Object: GanttResponse (non-persistent)]
   ↓
[3. Loop: Duyệt từng GanttTask → Build JSON item]
   ↓
[4. Ghép chuỗi JSON hoàn chỉnh]
   ↓
[5. Return: String JSONResult]
[End]
```

---

### Chi tiết từng bước:

#### Bước 2.1 - Retrieve (Lấy dữ liệu từ Oracle)

| Thiết lập | Giá trị |
|:---|:---|
| **Source** | From Database |
| **Entity** | `MyFirstModule.GanttTask` |
| **Sorting** | `ProjectTaskId` Ascending |
| **Variable name** | `GanttTaskList` |

> Sắp xếp theo `ProjectTaskId` tăng dần để đảm bảo thứ tự cha trước, con sau khi vẽ cây.

---

#### Bước 2.2 - Create Object: Biến đếm và chuỗi tích lũy

Tạo biến String để tích lũy JSON:
- **Activity:** `Create Variable`
- **Type:** String
- **Name:** `JsonItems`
- **Value:** `''` (chuỗi rỗng)

---

#### Bước 2.3 - Loop: Duyệt từng GanttTask

Kéo activity **Loop** vào Microflow:
- **Iterate over:** `$GanttTaskList`
- **Loop variable name:** `CurrentTask`

**Bên trong vòng lặp:**

1. **Create Variable** `IsFirst`:
   - Kiểm tra xem đây có phải phần tử đầu tiên không để thêm dấu phẩy `,` giữa các item.

2. **Create Variable** `StartDateStr`:
   ```
   formatDateTime([%CurrentTask/StartDate%], 'yyyy-MM-dd')
   ```

3. **Create Variable** `EndDateStr`:
   ```
   formatDateTime([%CurrentTask/EndDate%], 'yyyy-MM-dd')
   ```

4. **Create Variable** `ParentStr`:
   - Nếu `$CurrentTask/ParentProjectTaskId` = 0 hoặc rỗng → dùng `'null'`
   - Ngược lại → dùng `toString($CurrentTask/ParentProjectTaskId)`

5. **Create Variable** `IsOpenStr`:
   - Nếu `$CurrentTask/IsOpen` = true → `'true'`
   - Ngược lại → `'false'`

6. **Create Variable** `OneJsonItem` (1 object JSON):
   ```
   '{' +
   '"id":' + toString($CurrentTask/ProjectTaskId) + ',' +
   '"project_name":"' + $CurrentTask/ProjectName + '",' +
   '"start_date":"' + $StartDateStr + '",' +
   '"end_date":"' + $EndDateStr + '",' +
   '"duration":' + toString($CurrentTask/Duration) + ',' +
   '"progress":' + toString($CurrentTask/Progress) + ',' +
   '"parent":' + $ParentStr + ',' +
   '"open":' + $IsOpenStr + ',' +
   '"color":"' + $CurrentTask/Color + '",' +
   '"progressColor":"' + $CurrentTask/ProgressColor + '",' +
   '"type":"' + $CurrentTask/TaskType + '"' +
   '}'
   ```

7. **Change Variable** `JsonItems`:
   - **Action:** Set
   - **Value:**
     ```
     if $JsonItems = ''
     then $OneJsonItem
     else $JsonItems + ',' + $OneJsonItem
     ```

---

#### Bước 2.4 - Build chuỗi JSON hoàn chỉnh

Sau vòng lặp, tạo biến `JSONResult`:
```
'{"data":[' + $JsonItems + ']}'
```

---

#### Bước 2.5 - End Event

- **Return type:** String
- **Return value:** `$JSONResult`

---

## BƯỚC 3: Tạo Page `GanttPage`

### 3.1 - Tạo Page mới

- **Page name:** `GanttPage`
- **Layout:** `Atlas_Default` (hoặc layout tùy chọn)
- **Navigation:** Thêm vào Navigation Menu

---

### 3.2 - Đặt widget lên Page

**Cách A: Dùng HTML/JavaScript Snippet Widget (đơn giản nhất)**

1. Cài widget **HTML Snippet** từ Mendix Marketplace (nếu chưa có).
2. Kéo widget **HTML Snippet** vào trang.
3. Tạo một **Nanoflow** hoặc **Data View (Microflow)** để gọi `GetGanttJsonData` và truyền chuỗi JSON cho widget.

**Cách B: Dùng Custom Widget (chuyên nghiệp hơn)**

1. Sử dụng widget **`mx-gantt`** hoặc **`dhtmlx-gantt`** từ Marketplace.
2. Config Data Source của widget trỏ về Microflow `GetGanttJsonData`.

---

### 3.3 - Cấu hình Data View trên Page (Cách A)

1. Kéo **Data View** vào trang.
2. **Data Source:**
   - **Type:** Microflow
   - **Microflow:** `GetGanttJsonData`
3. Bên trong Data View, kéo **Text Area** hoặc **HTML Snippet** để hiển thị JSON.

---

## BƯỚC 4: Cấu hình JavaScript Gantt Render

Nếu dùng HTML Snippet widget, bạn cần nhúng thư viện Gantt và render:

### 4.1 - Thêm thư viện vào `index.html`

Mở file `theme/web/index.html` và thêm vào trong thẻ `<head>`:

```html
<!-- DHTMLX Gantt CDN -->
<link rel="stylesheet" href="https://cdn.dhtmlx.com/gantt/edge/dhtmlxgantt.css">
<script src="https://cdn.dhtmlx.com/gantt/edge/dhtmlxgantt.js"></script>
```

---

### 4.2 - HTML Snippet Widget Content

Trong **HTML Snippet**, điền nội dung:

```html
<div id="gantt_container" style="width:100%; height:600px;"></div>

<script>
  // Khởi tạo Gantt
  gantt.config.date_format = "%Y-%m-%d";
  gantt.config.xml_date = "%Y-%m-%d";

  // Cấu hình cột hiển thị bên trái
  gantt.config.columns = [
    { name: "project_name", label: "Tên công việc", width: 280, tree: true },
    { name: "start_date",   label: "Bắt đầu",       width: 100, align: "center" },
    { name: "duration",     label: "Số ngày",        width: 70,  align: "center" },
    { name: "progress",     label: "Tiến độ",        width: 80,  align: "center",
      template: function(task) {
        return Math.round(task.progress * 100) + "%";
      }
    }
  ];

  // Cấu hình màu theo type
  gantt.templates.task_class = function(start, end, task) {
    return task.type === 'milestone' ? 'milestone-task' :
           task.type === 'project'   ? 'project-task'   : 'normal-task';
  };

  // Cấu hình màu thanh theo trường color
  gantt.templates.task_row_class = function(start, end, task) {
    return "";
  };

  gantt.templates.task_text = function(start, end, task) {
    return task.project_name;
  };

  // Ánh xạ tên trường JSON sang tên trường Gantt
  gantt.config.task_attribute = "type";

  gantt.init("gantt_container");

  // Lấy JSON từ Mendix (được truyền qua thuộc tính của widget)
  // Thay YOUR_JSON_DATA bằng chuỗi JSON thực tế từ Microflow
  var ganttData = YOUR_JSON_DATA;

  // Parse và load data
  gantt.parse(ganttData);
</script>

<style>
  .milestone-task .gantt_task_content { background: #ffd54f !important; }
  .project-task   .gantt_task_content { background: #42a5f5 !important; }
  .normal-task    .gantt_task_content { background: #66bb6a !important; }
</style>
```

---

## BƯỚC 5: Kết nối JSON từ Microflow vào Widget

### Cách thực hiện trong Mendix:

1. Tạo Entity tạm (non-persistent) **`GanttResponse`**:
   - Attribute: `JsonData` (String, unlimited)

2. Sửa lại Microflow `GetGanttJsonData`:
   - Thay vì return String trực tiếp → **Create Object `GanttResponse`** và gán `JsonData = $JSONResult`
   - Return Type: `GanttResponse`

3. Trên Page:
   - **Data View** → Source: Microflow `GetGanttJsonData` → Type: `GanttResponse`
   - Bên trong Data View, kéo **Text Area** gắn với `GanttResponse/JsonData`
   - Dùng JavaScript (qua HTML Snippet) để đọc giá trị từ Text Area và gọi `gantt.parse()`

---

## Tóm tắt luồng hoàn chỉnh

```
Oracle DB
  └── MYFIRSTMODULE$GANTTTASK (24 records)
        ↓
  Mendix GanttTask Entity (Retrieve from DB)
        ↓
  Microflow: GetGanttJsonData
    ├── Retrieve GanttTaskList (sort by ProjectTaskId ASC)
    ├── Loop → Build từng JSON item
    └── Return: '{"data":[{...},{...},...]}' (String)
        ↓
  GanttResponse Entity (JsonData = chuỗi JSON)
        ↓
  Page: GanttPage
    └── Data View (Microflow = GetGanttJsonData)
          └── HTML Snippet Widget
                ├── Nhúng DHTMLX Gantt
                ├── Đọc JsonData từ Text Area ẩn
                └── gantt.parse(jsonData) → Render Chart
```

---

## Lưu ý quan trọng

> **Trường `project_name` thay vì `text`:** DHTMLX Gantt mặc định dùng trường `text` để hiển thị nhãn. Nếu bạn đặt tên là `project_name`, cần cấu hình thêm:
> ```javascript
> gantt.config.task_text = "project_name";
> // hoặc dùng template:
> gantt.templates.task_text = function(start, end, task) {
>   return task.project_name;
> };
> ```

> **Trường `parent`:** Giá trị `null` (cho task gốc) phải được truyền đúng dạng JSON `null`, không phải chuỗi `"null"`.

> **Định dạng ngày:** DHTMLX Gantt đọc ngày theo định dạng `"%Y-%m-%d"` nên chuỗi `"2025-01-06"` là chuẩn xác.

---

## PHỤ LỤC: JSON Mẫu dùng cho Export Mapping trong Mendix

> Copy đoạn JSON bên dưới và paste vào ô **JSON Snippet** khi tạo **JSON Structure** trong Mendix Studio Pro.
> Mendix sẽ tự động nhận diện cấu trúc và sinh ra sơ đồ ánh xạ (Export Mapping) tương ứng.

```json
{
  "data": [
    {
      "id": 1,
      "project_name": "Dự án Phát Triển Hệ Thống ERP",
      "start_date": "2025-01-06",
      "end_date": "2025-06-30",
      "duration": 175,
      "progress": 0.45,
      "parent": null,
      "open": true,
      "color": "#5c6bc0",
      "progressColor": "#3949ab",
      "type": "project"
    },
    {
      "id": 2,
      "project_name": "Giai đoạn 1: Phân tích & Thiết kế",
      "start_date": "2025-01-06",
      "end_date": "2025-01-31",
      "duration": 25,
      "progress": 1.0,
      "parent": 1,
      "open": true,
      "color": "#42a5f5",
      "progressColor": "#1e88e5",
      "type": "project"
    },
    {
      "id": 6,
      "project_name": "Thu thập yêu cầu nghiệp vụ",
      "start_date": "2025-01-06",
      "end_date": "2025-01-10",
      "duration": 5,
      "progress": 1.0,
      "parent": 2,
      "open": false,
      "color": "#66bb6a",
      "progressColor": "#43a047",
      "type": "task"
    },
    {
      "id": 10,
      "project_name": "Hoàn thành Phân tích & Thiết kế",
      "start_date": "2025-01-31",
      "end_date": "2025-01-31",
      "duration": 0,
      "progress": 1.0,
      "parent": 2,
      "open": false,
      "color": "#ffd54f",
      "progressColor": "#ffb300",
      "type": "milestone"
    }
  ]
}
```

### Hướng dẫn tạo JSON Structure & Export Mapping:

#### Tạo JSON Structure:
1. Chuột phải vào `MyFirstModule` → **Add other** → **JSON Structure**.
2. Đặt tên: `JSON_GanttStructure` → **OK**.
3. Paste đoạn JSON trên vào ô **JSON Snippet**.
4. Nhấn **Format** → **OK**.

#### Tạo Export Mapping:
1. Chuột phải vào `MyFirstModule` → **Add other** → **Export Mapping**.
2. Đặt tên: `Export_GanttData` → **OK**.
3. Trong màn hình Export Mapping:
   - **Schema source:** Chọn `JSON Structure` → Chọn `JSON_GanttStructure`.
   - Tick chọn tất cả các node: `data`, `(Object)` bên trong.
   - Nhấn **OK**.
4. Cửa sổ Mapping hiện ra:
   - **Node ngoài cùng `(Object)`** → Map với Entity `GanttResponse`.
   - **Node `data` (mảng)** → Map với Entity `GanttTask` (liên kết qua Association `GanttTask_GanttResponse`).
   - **Các trường bên trong:**

| JSON Key | Map tới Attribute |
|:---|:---|
| `id` | `GanttTask/ProjectTaskId` |
| `project_name` | `GanttTask/ProjectName` |
| `start_date` | `GanttTask/StartDate` |
| `end_date` | `GanttTask/EndDate` |
| `duration` | `GanttTask/Duration` |
| `progress` | `GanttTask/Progress` |
| `parent` | `GanttTask/ParentProjectTaskId` |
| `open` | `GanttTask/IsOpen` |
| `color` | `GanttTask/Color` |
| `progressColor` | `GanttTask/ProgressColor` |
| `type` | `GanttTask/TaskType` |

5. Nhấn **Save** để lưu Export Mapping.

---

### Lưu ý khi ánh xạ kiểu dữ liệu:

| JSON Type | Mendix Type | Ghi chú |
|:---|:---|:---|
| `number` (id, duration) | Integer | `ProjectTaskId`, `Duration` |
| `number` (progress) | Decimal | `Progress` (0.0 → 1.0) |
| `string` (start_date) | String → DateTime | Dùng `parseDateTime` trong Microflow để chuyển đổi |
| `boolean` (open) | Boolean | `IsOpen` |
| `null` (parent) | Integer | Khi parent = null → gán giá trị `0` hoặc `-1` để phân biệt node gốc |
| `string` (type) | String | `TaskType`: "project" / "task" / "milestone" |
