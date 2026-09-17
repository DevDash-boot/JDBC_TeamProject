# 코드 리뷰 — JDBC_TeamProject (무인편의점 재고관리)

리뷰 일자: 2026-09-17
리뷰 범위: `src/main/java/com/tenco` 의 dao, dto, Service, util, view, `Main2.java`, `convenienceStore.sql`, `build.gradle`, `.gitignore`, `README.md`, git 커밋 이력
리뷰 제외: `swing/` 패키지 6개 파일과 `Main.java` (AI 생성 Swing 화면)

---

## 1. 총평

구조만 놓고 보면 이 단계에서 기대하기 어려운 수준까지 올라와 있습니다. 트랜잭션 경계가 Service에 있고 DAO가 `Connection` 을 받아 쓰는 형태, `UPDATE ... WHERE stock >= ?` 로 락 없이 음수 재고를 막는 기법, 스키마를 저장소에 커밋해 누구나 재현할 수 있게 한 점, 자격증명을 첫 커밋부터 환경변수로 뺀 점은 모두 정석입니다. PR을 21번 머지하며 협업한 흔적도 분명합니다.

문제는 **그 좋은 구조가 일부 기능에서만 지켜졌다는 것**입니다. 같은 저장소 안에서 재고를 차감하는 코드가 두 벌인데 한쪽만 음수를 막고, 커넥션을 다루는 방식이 네 가지이며, 발주 쪽은 트랜잭션을 열어놓고 예외를 잘못 잡아 **재고는 그대로인 채 발주 기록만 지워집니다.** 실제 DB에서 재현했습니다.

가장 급한 것은 Blocker 4건입니다. 그중 두 건(로그아웃 후 관리자 권한 유지, 발주 취소 시 부분 커밋)은 보안과 데이터 정합성에 직접 닿아 있고, 나머지 두 건(발주 단건 조회가 다른 상품을 돌려줌, 수량 0 입력이 0원 주문을 만듦)은 사용자가 정상 메뉴를 누르면 바로 만나는 경로입니다.

테스트는 0건입니다. `src/test` 디렉터리 자체가 없습니다. Blocker 4건 모두 기능 하나당 테스트 한 개만 있었어도 걸렸을 성격입니다.

---

## 2. 검증 방법과 한계

### 실제로 실행해서 확인한 것

팀 DB(`192.168.5.4:3306`)는 제공받은 계정으로 접속이 거부되어(`Access denied for user 'root'`), **저장소에 커밋된 `convenienceStore.sql` 을 로컬 MySQL 8.0에 그대로 실행해 검증용 DB를 만들었습니다.** 그 뒤 프로젝트를 샌드박스로 복사하고 접속 URL만 `127.0.0.1` 로 바꿔, **학생 코드의 Service와 DAO를 그대로 호출해서** 아래를 확인했습니다. 원본 파일은 한 글자도 바꾸지 않았습니다.

| 항목 | 방법 | 결과 |
| :--- | :--- | :--- |
| 스키마 실행 | `mysql < convenienceStore.sql` | **성공**. 테이블 6개, 상품 11건, 주문 5건, 발주 6건 생성 |
| 컴파일 | Gradle 9.6.0 / JDK 21 `compileJava` | 통과 |
| 단위 테스트 | `src/test` 확인 | **테스트 0건** (디렉터리 없음) |
| 관리자 로그인 | `AuthService.authenticationAdmin("admin01","1234")` | 정상 로그인, 틀린 비밀번호는 정상 거부 |
| 발주 단건 조회 | `PurchaseService.existList(3)` | **다른 상품 반환** (BL-3) |
| 수량 0 주문 | `OrderService.processOrder` | **성공 처리됨**, 0수량 행 생성 (BL-4) |
| 발주 취소 | `PurchaseService.deletePurchase(3)` | **발주만 삭제, 재고 그대로** (BL-2) |
| 유통기한 NULL 상품 | `ProductDAO.getProduct()` | **NullPointerException** (MJ-2) |
| 재고 복구 후 상태 | `updateStatus()` → 발주 보충 | **status가 false로 고정** (MJ-3) |
| 주문 수량 변경 | `OrderService.updateOrderItemQuantity` | **재고 -9967** (MJ-1) |
| 중복 상품 행 | 같은 주문·같은 상품 2행 후 수량 변경 | **두 행이 함께 변경** (MJ-4) |
| 상품 삭제 | 주문 이력 있는 상품 삭제 | `RuntimeException` 이 View를 통과 (MJ-5) |
| 스키마 제약 | `information_schema` 조회 | 부록 A에 기재 |
| 커밋 이력 | `git log`, `git show`, `git ls-files` | 6장에 기재 |

### 확인하지 못한 것

- **팀 운영 DB(`192.168.5.4`)의 실제 데이터 상태** — **확인 필요**. 위 검증은 전부 `convenienceStore.sql` 로 만든 초기 상태 기준입니다. 운영 DB에는 이미 재고가 음수이거나 0수량 주문이 쌓여 있을 수 있습니다. 부록 A의 점검 쿼리로 확인하십시오.
- **Swing 화면의 동작** — 리뷰 범위에서 제외했습니다. 다만 Swing이 `OrderService.cancelOrder(orderId, itemList)` 같은 위험한 메서드를 호출하고 있다면 MJ-6이 실제 결함이 됩니다 — **확인 필요**.
- `Main.java`(Swing 진입점)가 `isAdmin` 권한을 어떻게 다루는지 — BL-1이 Swing에서도 성립하는지는 **확인 필요**.

---

## 3. 잘된 부분

### 3-1. 트랜잭션 경계를 Service에 두고 DAO에 Connection을 넘겼습니다

```java
// OrderItemService.java:41-48 — 경계는 Service
try (Connection conn = util.getConnection()) {
    conn.setAutoCommit(false);
    OrderItem orderItem = orderItemDAO.selectById(conn, orderItemId);
    ...
    productDAO.outStock(conn, orderItem.getProductId(), difference);
    orderItemDAO.updateOrderItem(conn, quantity, orderItemId);
    orderDAO.updateTotalPrice(conn, orderItem.getOrderId(), totalPrice);
    conn.commit();
```

```java
// ProductDAO.java:314 — DAO 는 받아서 쓰기만 한다
public int outStock(Connection conn, int productId, int quantity) throws SQLException {
```

**왜 잘한 것인가**: "이 작업이 하나로 묶여야 하는가"는 업무 규칙이고, 업무 규칙은 Service의 책임입니다. DAO가 스스로 커넥션을 열고 커밋하면 **여러 DAO를 한 트랜잭션으로 묶을 수 없습니다.** 이 프로젝트는 주문 수량을 바꿀 때 `order_item`, `product`, `orders` 세 테이블을 서로 다른 DAO로 건드리는데, Connection을 넘기는 구조라 하나의 트랜잭션으로 묶입니다. 이 형태는 나중에 프레임워크의 트랜잭션 기능을 배울 때 그대로 이어집니다.

### 3-2. 조건부 UPDATE로 음수 재고를 막았습니다

```java
// ProductDAO.java:314-322
public int outStock(Connection conn, int productId, int quantity) throws SQLException {
    String sql = "UPDATE product SET stock = stock - ? WHERE product_id = ? AND stock >= ?";
    ...
    pstmt.setInt(3, quantity); // WHERE 조건에도 넣어 동시성 이슈(마이너스 재고) 방지
```

**왜 잘한 것인가**: "재고를 읽고 → 자바에서 비교하고 → UPDATE" 순서로 하면, 읽은 시점과 쓰는 시점 사이에 다른 사람이 끼어들어 재고가 음수가 됩니다. 이를 경합 조건(race condition)이라고 합니다. 위 코드는 **비교와 차감을 SQL 한 문장 안에서 동시에** 처리합니다. UPDATE 한 문장은 DB가 원자적으로 처리하므로 끼어들 틈이 없고, 조건이 안 맞으면 0행이 반환되어 실패를 알 수 있습니다. 락을 걸지 않고 해결한 것이라 성능도 유리합니다. 주석에 의도까지 적어 둔 점이 좋습니다.

### 3-3. 스키마를 커밋했고, 그대로 실행됩니다

`src/main/java/com/tenco/convenienceStore.sql` 에 `CREATE DATABASE`, `CREATE TABLE`, 제약조건, 샘플 데이터, 관리자 계정까지 들어 있습니다. 이번 리뷰에서 이 파일 하나로 검증 환경을 만들었고 **오류 없이 실행되었습니다.**

**왜 잘한 것인가**: "clone 한 사람이 실행할 수 있는가"는 프로젝트 완성도의 실질적인 기준입니다. 코드가 아무리 좋아도 DB를 만들 수 없으면 아무도 돌려볼 수 없습니다. 게다가 해시 비밀번호를 넣는 `UPDATE` 문에 "이 것을 실행해야 해시로 비밀번호가 들어가서 정상작동됨" 이라는 안내까지 달아 두었습니다. 다음 사람을 배려한 흔적입니다.

### 3-4. 자격증명을 처음부터 환경변수로 분리했습니다

```java
// util.java:11-12
private static final String DB_USER = System.getenv("DB_USER");
private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");
```

커밋 이력 전체를 검색했으나 **비밀번호를 소스에 하드코딩한 커밋이 한 건도 없습니다.** 첫 커밋부터 `System.getenv` 입니다.

**왜 잘한 것인가**: git은 과거를 지우지 않습니다. 비밀번호를 한 번이라도 커밋하면 나중에 코드를 고쳐도 `git log -p` 한 번이면 드러나고, 공개 저장소라면 자동 수집 봇이 몇 분 안에 긁어 갑니다. 그래서 실무 원칙은 "유출된 자격증명은 지우는 게 아니라 무효화한다"이고, 애초에 넣지 않는 것이 유일하게 싼 해법입니다. 팀 전체가 이걸 지켰습니다.

### 3-5. 비밀번호를 해시로 저장하고 로그인 결과에서 지웁니다

```java
// AuthService.java:23-26
if (admin == null || !BCrypt.checkpw(password, admin.getPassword())) {
    return null;
}
admin.setPassword(null);
return admin;
```

**왜 잘한 것인가**: `BCrypt.checkpw` 로 해시 검증을 하고, 성공한 뒤에는 객체에서 비밀번호를 지워 화면 계층으로 넘깁니다. 세션 객체에 해시가 남아 있으면 `toString()` 한 번, 로그 한 줄로 그대로 새어 나갑니다. `getAllAdmin()` (`AuthService.java:54-59`)에서도 목록 전체를 돌며 비밀번호를 지웁니다. 빠뜨리기 쉬운 자리인데 두 곳 다 챙겼습니다.

### 3-6. 장바구니 담기가 실제 재고를 계속 반영합니다

```java
// OrderView.java:109-119
int cartQuantity = items.stream()
        .filter(item -> item.getProductId() == currentProductId)
        .mapToInt(OrderItem::getQuantity)
        .sum();
int availableStock = product.getStock() - cartQuantity;
```

**왜 잘한 것인가**: 재고 20개짜리 상품을 장바구니에 15개 담은 뒤 다시 10개를 담으려 하면 "추가 가능 최대 수량 5개"로 막습니다. DB 재고만 보고 판단했다면 25개를 담고 결제 단계에서 실패했을 것입니다. 아직 DB에 반영되지 않은 "담은 수량"을 따로 계산해 뺀 것은 장바구니의 본질을 이해했다는 뜻입니다.

### 3-7. PR 기반으로 협업했습니다

커밋 155개 중 **PR 머지가 21건**이고 PR 번호는 #30까지 올라갑니다. 기능 브랜치를 만들고 PR로 main에 합치는 흐름을 실제로 돌렸습니다.

**왜 잘한 것인가**: 전원이 main에 직접 커밋하는 방식보다 훨씬 낫습니다. PR은 "내 코드를 다른 사람이 볼 기회"를 만드는 장치이고, 실무에서 코드 리뷰가 붙는 지점이 정확히 여기입니다. 21번이면 습관이 붙을 만한 횟수입니다.

### 3-8. 그 밖에

- **빌드 재현성**: `gradle-wrapper.jar` 가 커밋되어 있어 Gradle이 설치되지 않은 컴퓨터에서도 `./gradlew` 한 줄로 돌아갑니다. `build.gradle` 의 `jar` 블록에 fat jar 설정(의존 라이브러리를 함께 묶고, 서명 파일을 제외)까지 되어 있어 배포 가능한 형태입니다.
- **SQL 인젝션 방어**: DAO 6개 전체에서 값을 SQL 문자열에 이어 붙인 곳이 없습니다. 검색의 LIKE도 `pstmt.setString(1, "%" + productName + "%")` 로 `%` 만 값에 붙였습니다.
- **트랜잭션 정리**: `finally` 에서 `setAutoCommit(true)` 로 되돌린 뒤 커넥션을 반납합니다. 풀에서 빌린 커넥션의 상태를 원래대로 돌려놓는 것까지 챙겼습니다.

---

## 4. 지적 사항

- **Blocker** : 실행하면 터지거나 데이터가 깨진다. 반드시 고쳐야 함
- **Major** : 지금은 동작하지만 조건이 바뀌면 깨진다. 고쳐야 함
- **Minor** : 동작에는 문제없으나 유지보수에 불리하다. 고치면 좋음
- **Nit** : 취향에 가까운 사소한 것. 참고만

---

### [Blocker] BL-1. 로그아웃해도 관리자 권한이 풀리지 않습니다

**위치**: `Main2.java:12, 54-58` 과 `view/AdminView.java:28, 46`

```java
// Main2.java:12
boolean isAdmin = false;
...
// Main2.java:54-58
boolean result = adminView.AdminStart();
if (result) {
    isAdmin = true;
    System.out.println("관리자 로그인 상태입니다.");
}
```

```java
// AdminView.java:26-29 — 로그인 성공 직후에만 true 를 돌려준다
case 1:
    loginMenu();
    if (isAdminLoggedIn()) {
        return true;
    }
// AdminView.java:44-46 — 그 외에는 항상 false
case 0:
    System.out.println("메인 메뉴로 돌아갑니다");
    return false;
```

**무엇이 문제인가**

권한 상태가 **두 곳에 따로 저장되어 있습니다.** `Main2` 의 지역변수 `isAdmin` 과 `AdminView` 의 필드 `currentAdminId` 입니다. 그런데 `AdminStart()` 는 "지금 로그인되어 있는가"가 아니라 **"방금 로그인에 성공했는가"** 만 돌려줍니다. 로그아웃은 `AdminView` 안에서만 일어나고, `Main2` 에는 그 사실이 전달될 길이 없습니다.

결정적으로 `Main2` 에는 `isAdmin = false;` 로 되돌리는 코드가 **어디에도 없습니다.** 한 번 `true` 가 되면 프로그램이 끝날 때까지 `true` 입니다.

**어떤 상황에서 터지는가**

1. 메뉴 5 → 1번 로그인 → 성공 → `AdminStart()` 가 `true` 반환 → `isAdmin = true`, 메인 메뉴로 나옴
2. 메뉴 5 → 4번 로그아웃 → "관리자님이 로그아웃되었습니다" 출력, `currentAdminId = null`
3. 메뉴 0 → `AdminStart()` 가 `false` 반환 → `if (result)` 가 거짓 → **`isAdmin` 은 그대로 `true`**
4. 메뉴 1(상품관리)과 메뉴 4(발주/입고)가 **그대로 열립니다.** 상품 등록·수정·삭제, 발주 신청·취소가 전부 가능합니다.

화면에는 "로그아웃되었습니다" 라고 나왔는데 권한은 살아 있습니다. 공용 PC에서 쓰는 무인편의점 관리 프로그램이라는 설정을 감안하면 그대로 두기 어려운 문제입니다.

> **검증 상태**: 이 항목은 키보드 입력이 필요한 메뉴 흐름이라 자동 실행으로 재현하지 않고 **코드 경로를 따라가 확인했습니다.** 다른 Blocker 3건과 달리 실행 로그가 없는 이유입니다. 위 1~4번 순서를 콘솔에서 직접 눌러 보시면 3분 안에 확인됩니다. `Main.java`(Swing)도 같은 구조인지는 **확인 필요**입니다.

**수정안**

권한 상태를 한 곳에서만 관리하고, 메뉴에 들어갈 때마다 현재 상태를 묻습니다.

```java
// AdminView — 현재 로그인 상태를 밖에서 물어볼 수 있게 공개
public boolean isLoggedIn() {
    return currentAdminId != null;
}

// Main2 — 지역변수 isAdmin 을 없애고 매번 확인
case 1:
    if (!adminView.isLoggedIn()) {
        System.out.println("관리자 로그인 필요");
        break;
    }
    new ProductView().start();
    break;
```

`AdminStart()` 의 반환값은 더 이상 권한 판단에 쓰지 않으므로 `void` 로 바꿔도 됩니다.

**왜 그렇게 고치는가**

같은 사실을 두 곳에 저장하면 한 곳만 바뀌는 순간이 반드시 옵니다. 여기서는 로그아웃이 그 순간이었습니다. **권한처럼 틀리면 안 되는 상태일수록 저장소를 하나로 두고, 필요할 때마다 그 하나에게 물어봐야 합니다.** "값을 받아서 보관"하는 대신 "필요할 때 조회"로 바꾸면 이런 종류의 어긋남이 구조적으로 생기지 않습니다.

---

### [Blocker] BL-2. 발주를 취소하면 재고는 그대로인데 발주 기록만 사라집니다

**위치**: `Service/PurchaseService.java:62-90`

```java
public void deletePurchase(int id) throws SQLException {
    ...
    conn.setAutoCommit(false);
    PurchaseDto purchaseDto = purchaseDAO.deletePurchase(conn, id);          // 발주 행 삭제
    productDAO.outStock(conn, purchaseDto.getProductId(), purchaseDto.getQauntity());  // 재고 차감
    System.out.println("발주목록에서 삭제 되었습니다. 발주취소 되었습니다. ---  발주ID : " + id);
    conn.commit();
} catch (RuntimeException e) {          // ← SQLException 을 잡지 않는다
    if (conn != null) conn.rollback();
    ...
} finally {
    if (conn != null) {
        conn.setAutoCommit(true);       // ← 여기서 커밋된다
        conn.close();
    }
}
```

**무엇이 문제인가**

두 가지가 겹쳐 있습니다.

1. **`outStock` 의 반환값을 확인하지 않습니다.** `outStock` 은 `WHERE ... AND stock >= ?` 조건이 걸려 있어(3-2에서 칭찬한 그 코드입니다) 재고가 발주 수량보다 적으면 **아무 행도 바꾸지 않고 0을 반환합니다.** 예외는 나지 않습니다. 호출하는 쪽이 0을 확인하지 않으면 실패한 줄도 모릅니다.

2. **`catch (RuntimeException e)` 는 `SQLException` 을 잡지 못합니다.** `SQLException` 은 `RuntimeException` 이 아니라 `Exception` 의 자식입니다. DAO가 `SQLException` 을 던지면 이 catch를 그냥 지나쳐 `finally` 로 갑니다. 그런데 `finally` 의 `conn.setAutoCommit(true)` 는 **JDBC 규칙상 진행 중인 트랜잭션을 커밋합니다.** 롤백하려던 작업이 오히려 확정됩니다.

**실제로 재현했습니다**

발주 3번은 감자칩(상품 4번) 120개 발주인데, 감자칩의 현재 재고는 28개입니다. 120개를 빼려면 재고가 음수가 되므로 `outStock` 이 막습니다.

```
[실행 전]
  purchase_id=3 | product_id=4 | quantity=120
  product_id=4 | product_name=감자칩 | stock=28

발주목록에서 삭제 되었습니다. 발주취소 되었습니다. ---  발주ID : 3
  -> 예외 없이 정상 종료됨

[실행 후]
  purchase_id=3 행: (없음 — 삭제됨)
  product_id=4 | product_name=감자칩 | stock=28   ← 그대로
```

사용자에게는 "발주취소 되었습니다" 라고 알렸고, 발주 기록은 사라졌고, 재고는 한 개도 줄지 않았습니다. **장부와 실물이 어긋난 상태이고, 되돌릴 근거가 되는 발주 기록마저 없어졌습니다.**

**수정안**

```java
public void deletePurchase(int id) throws SQLException {
    if (id <= 0) throw new IllegalArgumentException("유효한 ID를 입력해주세요.");

    Connection conn = null;
    try {
        conn = util.getConnection();
        conn.setAutoCommit(false);

        PurchaseDto purchaseDto = purchaseDAO.deletePurchase(conn, id);

        int rows = productDAO.outStock(conn, purchaseDto.getProductId(), purchaseDto.getQauntity());
        if (rows == 0) {                                  // 반환값 확인
            throw new SQLException("현재 재고(" + purchaseDto.getQauntity()
                    + "개 미만)보다 발주 수량이 많아 취소할 수 없습니다.");
        }

        conn.commit();
    } catch (SQLException e) {                            // SQLException 을 잡는다
        if (conn != null) conn.rollback();
        throw e;
    } finally {
        if (conn != null) {
            conn.rollback();                              // 커밋되지 않은 것은 여기서 확실히 되돌린다
            conn.setAutoCommit(true);
            conn.close();
        }
    }
}
```

`finally` 에서 `setAutoCommit(true)` **앞에** `rollback()` 을 한 번 더 두는 것이 안전합니다. 이미 커밋된 트랜잭션에 대한 `rollback()` 은 아무 일도 하지 않으므로 부작용이 없습니다.

**왜 그렇게 고치는가**

두 가지 원칙입니다. 첫째, **`executeUpdate` 의 반환값은 "몇 행이 실제로 바뀌었는가"라는 중요한 정보이므로 버리면 안 됩니다.** 조건부 UPDATE를 쓴 순간부터 0행은 정상 결과가 아니라 실패 신호입니다. 둘째, **`catch` 에 적는 예외 타입은 실제로 던져지는 타입이어야 합니다.** 여기서는 `RuntimeException` 이라고 적어 두었지만 그 자리에 오는 것은 `SQLException` 이라, 예외 처리 코드가 통째로 죽어 있었습니다. IDE가 경고해 주지 않는 종류의 실수라 더 위험합니다.

같은 파일 안의 `purcahseProduct`(`:34-60`)는 `catch (SQLException e)` 로 제대로 잡고 있습니다. 두 메서드가 나란히 있는데 한쪽만 틀린 것이라, 비교해 보면 바로 보입니다.

---

### [Blocker] BL-3. 발주 단건 조회가 엉뚱한 상품의 발주를 돌려줍니다

**위치**: `dao/PurchaseDAO.java:45-71`

```java
public PurchaseDto existList(int id) throws SQLException {
    String alreadySql = """
            select p.* , pr.product_name
            from purchase p join product pr on p.product_id = pr.product_id
            where p.purchase_id = ?          // ← purchase_id 로 찾는다
            """;

    // 상품 테이블에도 실제 존재하는 상품인지 확인.
    Product product = new ProductDAO().selectProductById(conn, id);   // ← product_id 로 찾는다
    if (product == null) throw new SQLException("ID가 " + id + "인 상품은 아예 존재하지 않습니다.");
    ...
```

**무엇이 문제인가**

`id` 라는 하나의 값을 **`product_id` 로도 쓰고 `purchase_id` 로도 씁니다.** 존재 확인은 상품 번호로 하고, 실제 조회는 발주 번호로 합니다. 이름이 그냥 `id` 라서 어느 쪽인지 알 수 없고, 호출하는 쪽마다 다르게 해석합니다.

실제로 화면의 안내 문구도 갈립니다. `PurchaseView` 의 메뉴는 "2.상품 ID로 발주 조회" 인데(`:19`), 바로 다음 줄의 입력 프롬프트는 "조회할 발주 ID를 입력해주세요"(`:35`) 입니다. **만든 사람도 무엇을 받는지 확신하지 못한 상태**라는 뜻입니다.

**실제로 재현했습니다**

상품 3번(생수)의 발주 내역을 보려고 `3` 을 입력한 경우입니다.

```
[DB 실제 발주 목록]
  purchase_id=1 -> product_id=8  (컵라면)   qty=20
  purchase_id=2 -> product_id=6  (샌드위치) qty=10
  purchase_id=3 -> product_id=4  (감자칩)   qty=120
  purchase_id=4 -> product_id=3  (생수)     qty=30
  ...

[코드가 돌려준 결과] 감자칩 / purchaseId=3 / productId=4 / qty=120
>> 기대: 생수(product_id=3) / 실제: 감자칩
```

오류도 경고도 없이 **다른 상품의 발주 정보**가 나옵니다. 상품 3번은 실제로 존재하므로 존재 확인도 통과합니다. 재고 관리 프로그램에서 남의 발주 수량을 보고 판단하면 그 뒤의 모든 결정이 틀어집니다.

**수정안**

무엇으로 찾을지 정하고, 이름에 그것을 적습니다.

```java
// 발주 번호로 찾는 경우
public PurchaseDto findByPurchaseId(int purchaseId) throws SQLException {
    String sql = """
            select p.*, pr.product_name
            from purchase p join product pr on p.product_id = pr.product_id
            where p.purchase_id = ?
            """;
    // 상품 존재 확인은 JOIN 이 이미 보장하므로 불필요
    ...
}

// 상품 번호로 그 상품의 발주 내역을 모두 찾는 경우 (여러 건일 수 있으므로 List)
public List<PurchaseDto> findByProductId(int productId) throws SQLException {
    String sql = """
            select p.*, pr.product_name
            from purchase p join product pr on p.product_id = pr.product_id
            where p.product_id = ?
            """;
    ...
}
```

**왜 그렇게 고치는가**

`id` 라는 이름은 아무것도 알려주지 않습니다. **이름이 모호하면 부르는 쪽마다 다르게 해석하고, 컴파일러는 둘 다 `int` 라서 막아 주지 않습니다.** `purchaseId`, `productId` 로 적으면 메서드 시그니처만 보고도 무엇을 넘길지 알 수 있고, 화면의 안내 문구도 자동으로 정해집니다. 한 메서드 안에서 같은 변수를 두 가지 의미로 쓰고 있다면 그건 메서드를 둘로 나누라는 신호입니다.

덧붙여 `JOIN product` 가 이미 있으므로 `selectProductById` 로 존재를 다시 확인하는 부분은 필요 없습니다. JOIN이 안 걸리면 결과가 비어서 나옵니다.

---

### [Blocker] BL-4. 수량 입력에서 0(취소)을 누르면 0원짜리 주문이 만들어집니다

**위치**: `view/OrderView.java:128-166`

```java
while (true) {
    quantity = Integer.parseInt(scanner.nextLine().trim());
    if (quantity == 0) {
        System.out.println(" -> 상품 선택을 취소했습니다.");
        break;                    // ← 수량 입력 루프만 빠져나온다
    }
    ...
}

// 중복 상품 합산 처리        ← 취소했는데 여기로 그대로 내려온다
Optional<OrderItem> existItem = items.stream()...;
if (existItem.isPresent()) {
    item.setQuantity(item.getQuantity() + quantity);   // + 0
} else {
    OrderItem item = new OrderItem(productId, quantity, product.getPrice());  // quantity = 0
    items.add(item);                                    // 장바구니에 0개짜리가 담긴다
}
```

**무엇이 문제인가**

수량에 `0` 을 입력하면 "상품 선택을 취소했습니다" 라고 출력하고 `break` 하는데, 이 `break` 는 **수량 입력 루프만** 빠져나옵니다. 그 아래의 장바구니 담기 코드는 그대로 실행되어 **수량 0짜리 항목이 담깁니다.** 취소가 취소로 동작하지 않습니다.

**실제로 재현했습니다**

```
[주문 전] order_item 건수 = 9
processOrder 결과 = true
[주문 후] order_item 건수 = 10
수량 0 인 order_item 건수 = 1
product 2 | 콜라 | stock=30  ← 재고는 변동 없음
```

주문이 **성공 처리**되어 `orders` 에 0원 주문이, `order_item` 에 수량 0인 행이 남았습니다. 재고는 줄지 않았으니 실물과는 맞지만, 장부에는 아무도 사지 않은 주문이 기록됩니다. 이런 행이 쌓이면 매출 집계와 주문 조회가 전부 오염됩니다.

**수정안**

취소 여부를 표시하는 값을 두고, 장바구니 담기 전에 건너뜁니다.

```java
int quantity = 0;
boolean cancelled = false;
while (true) {
    try {
        quantity = Integer.parseInt(scanner.nextLine().trim());
        if (quantity == 0) {
            System.out.println(" -> 상품 선택을 취소했습니다.");
            cancelled = true;
            break;
        }
        ...
        break;
    } catch (NumberFormatException e) {
        System.out.println("숫자를 입력해주세요");
    }
}
if (cancelled) {
    continue;      // 바깥 장바구니 루프의 처음으로 — 다음 상품 ID 입력 단계로 돌아간다
}
```

Service 쪽에도 방어를 하나 더 두는 편이 안전합니다.

```java
// OrderService.processOrder 안, 반복문 첫머리
if (itemDTO.getQuantity() <= 0) {
    conn.rollback();
    return false;
}
```

**왜 그렇게 고치는가**

`break` 는 **가장 안쪽 반복문 하나만** 빠져나옵니다. 중첩된 반복문에서 "이 항목은 건너뛴다"를 표현하려면 바깥 루프의 `continue` 가 필요하고, 그러려면 안쪽에서 바깥으로 의도를 전달할 값이 있어야 합니다. 중첩이 깊어질수록 이런 실수가 늘어나므로, 장바구니 담기 한 건을 별도 메서드로 빼서 `return` 으로 빠져나오게 만드는 것도 좋은 방법입니다.

그리고 화면에서 막는 것과 별개로 **Service에도 수량 검사를 두어야 합니다.** 화면은 여러 개가 될 수 있지만(이 프로젝트는 이미 콘솔과 Swing 두 벌입니다) Service는 하나이므로, 마지막 방어선은 Service에 있어야 합니다.

---

### [Major] MJ-1. 재고를 차감하는 코드가 두 벌인데 한쪽만 음수를 막습니다

**위치**: `dao/OrderDAO.java:109` vs `dao/ProductDAO.java:315`

```java
// OrderDAO.updateOrderItemAndStock:109 — 조건 없음
String updateStockSql = "UPDATE product SET stock = stock - ? WHERE product_id = ?";

// ProductDAO.outStock:315 — 조건 있음
String sql = "UPDATE product SET stock = stock - ? WHERE product_id = ? AND stock >= ?";
```

**무엇이 문제인가**

3-2에서 칭찬한 `AND stock >= ?` 방어가 **주문 수량 변경 경로에는 빠져 있습니다.** 같은 팀이 같은 일을 하는 SQL을 두 번 썼는데 한 번만 방어를 넣었습니다.

**실제로 재현했습니다**

`OrderView` 는 호출 전에 `newQuantity > maxAvailableStock` 을 검사하므로 콘솔에서는 막힙니다. 그래서 Service를 직접 호출해 화면 검사를 건너뛰어 보았습니다.

```
[실행 전] order_id=1 | product_id=2 | quantity=2 | stock=30
updateOrderItemQuantity(수량 2 -> 9999) 결과 = true
[실행 후] order_id=1 | product_id=2 | quantity=9999 | stock=-9967
          order_id=1 | total_price=19999500
```

**재고가 -9967개**가 되었고, 메서드는 `true`(성공)를 돌려주었습니다. 총액도 약 2천만 원으로 바뀌었습니다.

지금 콘솔 화면이 막아 주고 있을 뿐이고, **방어가 화면에만 있습니다.** 이 프로젝트에는 이미 Swing이라는 두 번째 화면이 있고, 그쪽이 같은 검사를 하고 있는지는 이 리뷰 범위 밖입니다 — **확인 필요**.

**수정안**

`outStock` / `inStock` 을 재사용하고, 반환값을 확인합니다.

```java
// OrderDAO 에서 직접 SQL 을 쓰지 말고
if (quantityDiff > 0) {
    if (productDAO.outStock(conn, productId, quantityDiff) == 0) {
        throw new SQLException("재고가 부족합니다.");
    }
} else if (quantityDiff < 0) {
    productDAO.inStock(conn, productId, -quantityDiff);
}
```

`OrderItemService.updateOrderItem`(`:52-70`)이 이미 정확히 이 형태로 되어 있습니다. 같은 저장소 안에 올바른 예가 있으므로 그대로 맞추면 됩니다.

**왜 그렇게 고치는가**

같은 일을 하는 코드가 두 벌 있으면 **한쪽만 고쳐지는 날이 반드시 옵니다.** 재고 차감처럼 규칙이 하나인 동작은 메서드 하나로 모아 두고 모두가 그것을 부르게 해야 합니다. 그러면 방어를 한 곳에만 넣어도 전부에 적용됩니다.

---

### [Major] MJ-2. 유통기한이 비어 있으면 상품 조회가 통째로 멈춥니다

**위치**: `dao/ProductDAO.java:271` (문제) vs `:299-303` (올바른 처리)

```java
// createProduct:271 — NULL 검사 없음
.expirationDate(rs.getDate("expiration_date").toLocalDate())

// selectProductById:299-303 — NULL 검사 있음
.expirationDate(
        rs.getDate("expiration_date") != null
                ? rs.getDate("expiration_date").toLocalDate()
                : null
)
```

**무엇이 문제인가**

스키마에서 `expiration_date DATE` 는 **NULL을 허용합니다**(`NOT NULL` 이 없습니다). 그런데 `createProduct` 는 NULL 검사 없이 `.toLocalDate()` 를 호출합니다. 같은 파일 안의 `selectProductById` 는 제대로 검사합니다. 두 메서드가 같은 컬럼을 정반대로 다룹니다.

`createProduct` 는 전체 조회, 상품명 검색, ID 검색, 재고 부족 조회 **네 기능이 공유**합니다.

**실제로 재현했습니다**

유통기한이 NULL인 상품을 한 건 넣고 전체 조회를 호출했습니다.

```
getProduct() 예외 -> java.lang.NullPointerException :
  Cannot invoke "java.sql.Date.toLocalDate()" because the return value of
  "java.sql.ResultSet.getDate(String)" is null

selectProductById(트랜잭션용)은 정상 = true
```

**상품 한 건 때문에 상품 목록 전체가 뜨지 않습니다.** 게다가 `ProductView.start()` 는 `catch (SQLException e)` 만 잡으므로 `NullPointerException` 은 그대로 통과해 상품관리 메뉴에서 튕겨 나갑니다.

지금은 앱으로 상품을 등록할 때 유통기한을 반드시 입력받아서 NULL이 들어가지 않습니다. 다만 그건 화면이 막고 있는 것이지 구조가 막는 것이 아닙니다. SQL로 직접 넣거나, 유통기한 없는 상품(생활용품 등)을 다루게 되는 순간 터집니다. **운영 DB에 이미 NULL이 있는지는 확인 필요** — 부록 A의 점검 쿼리를 쓰십시오.

반대 방향도 있습니다. `addProduct:43` 과 `updateProduct:96` 의 `Date.valueOf(product.getExpirationDate())` 는 DTO의 날짜가 `null` 이면 `NullPointerException` 을 던집니다.

**수정안**

```java
// 읽을 때
java.sql.Date d = rs.getDate("expiration_date");
.expirationDate(d == null ? null : d.toLocalDate())

// 쓸 때
if (product.getExpirationDate() == null) {
    pstmt.setNull(4, java.sql.Types.DATE);
} else {
    pstmt.setDate(4, Date.valueOf(product.getExpirationDate()));
}
```

유통기한이 반드시 있어야 하는 업무 규칙이라면, 반대로 **스키마를 `expiration_date DATE NOT NULL` 로 바꾸는 것**이 더 확실합니다. 규칙을 DB에 적어 두면 화면이 몇 개든 지켜집니다.

**왜 그렇게 고치는가**

"이 컬럼에 NULL이 들어올 수 있는가"는 스키마가 이미 답을 갖고 있습니다. **스키마가 NULL을 허용하면 코드도 NULL을 다뤄야 하고, 다루기 싫으면 스키마에서 막아야 합니다.** 둘 중 하나를 선택하지 않고 "실제로는 안 들어올 것"에 기대면, 언젠가 들어오는 날 기능 하나가 통째로 멈춥니다.

---

### [Major] MJ-3. 재고를 다시 채워도 판매 중지 상태가 풀리지 않습니다

**위치**: `dao/ProductDAO.java:231-238`

```java
public int updateStatus() {
    String sql = """
            UPDATE product SET status = 0 WHERE stock = 0
            """;
```

**무엇이 문제인가**

재고가 0이 되면 `status` 를 `false` 로 내리는 코드는 있는데, **재고가 다시 채워졌을 때 `true` 로 올리는 코드가 없습니다.** 한 방향으로만 갑니다.

**실제로 재현했습니다**

젤리(상품 11번)는 초기 데이터에서 재고 0, status false입니다. 발주로 10개를 채워 보았습니다.

```
[초기]           product_id=11 | 젤리 | stock=0  | status=0
updateStatus() 후 product_id=11 | 젤리 | stock=0  | status=0
발주로 10개 보충 후 product_id=11 | 젤리 | stock=10 | status=0   ← 여전히 0
```

재고가 10개 있는데 판매 중지 상태로 남습니다. 발주 담당자는 재고를 채웠으니 팔릴 거라고 생각하지만 상태는 그대로입니다.

**수정안**

한 문장으로 양방향 모두 맞춥니다.

```sql
UPDATE product SET status = (stock > 0)
```

더 나은 방향은 **`status` 를 저장하지 않는 것**입니다. `status` 는 `stock > 0` 에서 계산되는 값이고, 계산으로 얻을 수 있는 값을 따로 저장하면 두 값이 어긋나는 순간이 반드시 옵니다. 지금이 그 상태입니다.

```sql
SELECT product_id, product_name, price, stock, (stock > 0) AS available
FROM product
```

"단종" 처럼 재고와 무관한 의미가 필요하다면 컬럼 이름을 `is_discontinued` 로 분리하고, 판매 가능 여부는 재고로만 판단하는 편이 낫습니다.

**왜 그렇게 고치는가**

같은 사실을 두 곳(`stock` 과 `status`)에 저장하면 한 곳만 바꾸는 코드가 생깁니다. 이 프로젝트에서 재고를 바꾸는 곳은 `outStock`, `inStock`, `addAmount`, `updateOrderItemAndStock` 네 군데인데 **그중 어디도 `status` 를 건드리지 않습니다.** 네 곳 전부에 `status` 갱신을 추가하는 것보다, 저장을 그만두고 조회할 때 계산하는 쪽이 훨씬 안전합니다.

---

### [Major] MJ-4. 같은 주문에 같은 상품이 두 줄이면 수량 변경이 두 줄을 함께 바꿉니다

**위치**: `dao/OrderDAO.java:105` / 스키마의 `order_item` 테이블

```java
String updateItemSql = "UPDATE order_item SET quantity = ? WHERE order_id = ? AND product_id = ?";
```

**무엇이 문제인가**

`order_item` 에는 `(order_id, product_id)` 조합에 대한 UNIQUE 제약이 없습니다(확인 완료 — 인덱스는 PK와 FK용 두 개뿐). 그런데 위 UPDATE는 그 조합으로 **한 줄만 있다고 가정**합니다.

`OrderView` 의 장바구니는 같은 상품을 합산하므로 정상 경로에서는 한 줄입니다. 그러나 이를 보장하는 것이 **화면의 자바 코드일 뿐 DB가 아닙니다.** 다른 화면(Swing), 직접 INSERT, 또는 장바구니 로직이 바뀌는 순간 두 줄이 생깁니다.

**실제로 재현했습니다**

주문 2번에 상품 3번을 한 줄 더 넣은 뒤 수량 변경을 호출했습니다.

```
[변경 전]
  order_item_id=3  | order_id=2 | product_id=3 | quantity=2
  order_item_id=11 | order_id=2 | product_id=3 | quantity=5
[수량 7 로 변경 호출 후]
  order_item_id=3  | order_id=2 | product_id=3 | quantity=7   ← 둘 다
  order_item_id=11 | order_id=2 | product_id=3 | quantity=7   ← 바뀜
```

두 줄이 모두 7이 되어 주문 수량이 14개가 되었습니다. 반면 재고는 한 번만 차감되었습니다. **주문 내역과 재고가 어긋납니다.**

**수정안**

두 가지를 함께 합니다. 첫째, DB에 제약을 겁니다.

```sql
ALTER TABLE order_item ADD UNIQUE KEY uk_order_product (order_id, product_id);
```

둘째, UPDATE의 조건을 기본키로 바꿉니다.

```java
String updateItemSql = "UPDATE order_item SET quantity = ? WHERE order_item_id = ?";
```

`OrderItemDAO.updateOrderItem`(`:71-82`)이 이미 `order_item_id` 로 갱신하고 있습니다. 같은 저장소 안에 올바른 예가 있습니다.

**왜 그렇게 고치는가**

**한 행을 지목할 때는 기본키를 쓰는 것이 가장 안전합니다.** 기본키는 정의상 한 행만 가리키므로 몇 행이 걸릴지 걱정할 필요가 없습니다. 그리고 "같은 주문에 같은 상품은 한 줄뿐"이 업무 규칙이라면 그 규칙은 DB 제약으로 적어 두어야 합니다. 화면 코드의 규칙은 화면이 늘어날 때마다 다시 지켜야 하지만, 제약조건은 한 번 걸면 모든 경로에서 지켜집니다.

---

### [Major] MJ-5. 삭제할 수 없는 상품을 지우려 하면 메뉴에서 튕겨 나갑니다

**위치**: `dao/ProductDAO.java:128` → `view/ProductView.java:48`

```java
// ProductDAO.deleteProduct — 모든 SQLException 을 RuntimeException 으로 바꿔 던진다
} catch (SQLException e) {
    throw new RuntimeException(e);
}

// ProductView.start:48 — SQLException 만 잡는다
} catch (SQLException e) {
    System.out.println("오류: " + e.getMessage());
}
```

**무엇이 문제인가**

`order_item` 과 `purchase` 는 `product` 를 참조하는 외래키를 갖고 있고 삭제 규칙은 `NO ACTION` 입니다(확인 완료). 즉 **주문되었거나 발주된 적이 있는 상품은 삭제할 수 없고**, DB가 제약 위반 예외를 던집니다.

DAO는 이를 `RuntimeException` 으로 감싸 던지는데, View는 `SQLException` 만 잡습니다. `RuntimeException` 은 그대로 통과해 `Main2` 의 `catch (Exception e)` 까지 올라갑니다.

**실제로 재현했습니다**

```
주문 내역이 있는 상품(id=1) 삭제 시도
  -> java.lang.RuntimeException (View 의 catch(SQLException)로는 못 잡음)
     원인: SQLIntegrityConstraintViolationException
```

사용자 입장에서는 상품 삭제를 눌렀더니 **상품관리 메뉴가 통째로 닫히고 메인 메뉴로 돌아갑니다.** 화면에는 영어로 된 DB 원문 메시지가 나옵니다. 왜 삭제가 안 되는지(주문 이력이 있어서)는 알 수 없습니다.

**수정안**

원인을 구분해서 사람이 읽을 메시지로 바꿉니다.

```java
// ProductDAO
} catch (SQLIntegrityConstraintViolationException e) {
    throw new IllegalStateException("주문 또는 발주 이력이 있는 상품은 삭제할 수 없습니다.", e);
} catch (SQLException e) {
    throw new RuntimeException("상품 삭제 중 DB 오류", e);
}
```

View의 `catch` 도 함께 넓힙니다.

```java
} catch (SQLException | RuntimeException e) {
    System.out.println("오류: " + e.getMessage());
}
```

업무적으로는 **삭제 대신 `status` 를 내려 "판매 중지"로 두는 방식**(소프트 삭제)이 맞습니다. 이미 팔린 상품의 주문 내역은 남아야 하므로, 편의점 재고 시스템에서 상품을 물리적으로 지우는 일은 실제로는 거의 없습니다.

**왜 그렇게 고치는가**

예외를 감쌀 때 `RuntimeException` 으로 뭉뚱그리면 **호출하는 쪽이 원인을 구분할 수 없고, `catch` 에 무엇을 적어야 할지도 알 수 없습니다.** 이 프로젝트에서 예외 타입이 계층마다 바뀌는 곳이 여러 군데라(BL-2도 같은 뿌리입니다) "DAO는 어떤 예외를 던지는가"를 팀 규칙으로 한 번 정하고 가는 것이 좋습니다.

---

### [Major] MJ-6. 쓰이지 않는 두 번째 주문취소 경로가 남아 있습니다

**위치**: `Service/OrderService.java:214-231` / `dao/OrderDAO.java:139-166`

```java
// 주문 취소가 두 벌이다
public boolean cancelOrder(int orderId) { ... }                          // :127 — 정상 (재고복구 + order_item 삭제 + orders 삭제)
public boolean cancelOrder(int orderId, List<OrderItem> itemList) { ... } // :214 — 재고만 복구하고 아무것도 지우지 않음
```

```java
// OrderDAO.cancelOrderTransaction:139-166 — 주문 상태 변경 부분이 통째로 주석
// TODO - status  제거
//        try (PreparedStatement orderStmt = conn.prepareStatement(updateOrderSql)) {
//            ...
//        }
```

**무엇이 문제인가**

2개 인자를 받는 `cancelOrder` 는 **재고만 복구하고 주문과 주문상품을 지우지 않습니다.** 원래는 `orders.status` 를 `'CANCELLED'` 로 바꾸는 코드가 있었는데, `status` 컬럼이 DB에 없어서 주석 처리되었고, 그 결과 "재고만 늘리는 메서드"가 되었습니다.

`OrderView:451` 은 1개 인자 버전을 부르므로 콘솔에서는 문제가 없습니다. 하지만 이 메서드가 호출되면 **주문은 그대로 남고 재고만 늘어납니다.** 같은 주문을 두 번 취소하면 재고가 두 번 늘어납니다.

**어떤 상황에서 터지는가**

- Swing 화면이 이 메서드를 부르고 있다면 지금 바로 터집니다 — **확인 필요**
- 부르지 않더라도, 이름이 `cancelOrder` 라서 다음 사람이 자연스럽게 고를 수 있습니다. 두 개 중 어느 쪽이 맞는지 이름만으로는 알 수 없습니다

**수정안**

쓰지 않는 쪽을 **지웁니다.** `OrderService.cancelOrder(int, List)` 와 `OrderDAO.cancelOrderTransaction` 을 모두 삭제하고, `Order` DTO의 `status` 필드(`Order.java:19`)와 관련 생성자, `OrderView` 의 주석 처리된 `status` 코드(`:232`, `:240`, `:295`, `:415`, `:450`)도 함께 정리합니다.

**왜 그렇게 고치는가**

주석 처리한 코드를 남겨두는 이유는 대개 "나중에 쓸지도 몰라서"인데, **그 나중은 거의 오지 않고 대신 읽는 사람을 혼란스럽게 만듭니다.** 지운 코드가 다시 필요하면 git에서 꺼내면 됩니다. 그러라고 커밋 이력이 있는 것입니다. 특히 지금처럼 **같은 이름의 메서드 두 개가 서로 다르게 동작하는 상태**는 다음 사람이 잘못 고를 확률이 높아 위험합니다.

---

### [Major] MJ-7. 발주 신청이 항상 새 줄을 추가합니다 — 주석은 수정한다고 되어 있습니다

**위치**: `dao/PurchaseDAO.java:79-131`

```java
public void purchaseProduct(Connection conn , int id, int quantity) throws SQLException {
    // 새로 발주하는 상품이라면 추가(insert) , 기존에 있던 발주상품이라면 수정(update)
    //            ↑ 주석은 이렇게 적혀 있지만
    ...
    String newPurchaseSql = """
                    insert into purchase(product_id , quantity , unit_price , total_price)
                    values(? , ? , ? , ?);
                    """;
    //            ↑ 실제로는 항상 INSERT 만 한다
```

**무엇이 문제인가**

주석은 "기존에 있던 발주상품이라면 수정(update)" 이라고 선언하는데 코드에는 UPDATE가 없습니다. 같은 상품을 세 번 발주하면 `purchase` 테이블에 세 줄이 생깁니다.

그 결과 발주 취소(BL-2)와 수량 차감이 **어느 줄을 가리키는지 모호해집니다.** 감자칩을 20개씩 세 번 발주했다면 발주 번호가 세 개 생기고, 취소하려면 어느 번호인지 사용자가 직접 골라야 합니다. 발주 목록 화면은 같은 상품이 여러 줄로 보입니다.

**수정안**

어느 쪽이 업무 규칙인지 먼저 정해야 합니다.

- **발주는 매번 별개의 건이다** (권장) — 현재 코드가 맞습니다. 주석을 코드에 맞게 고치고, `purchase` 테이블에 `purchase_date` 컬럼을 추가해 언제 발주한 건인지 남깁니다. 발주 이력은 원래 누적되는 것이 자연스럽습니다.
- **상품당 발주는 한 줄이다** — 주석대로 하려면 `UNIQUE (product_id)` 를 걸고 아래처럼 씁니다.

```sql
INSERT INTO purchase(product_id, quantity, unit_price, total_price)
VALUES (?, ?, ?, ?)
ON DUPLICATE KEY UPDATE
    quantity = quantity + VALUES(quantity),
    total_price = total_price + VALUES(total_price)
```

**왜 그렇게 고치는가**

**주석과 코드가 어긋나면 주석이 거짓말을 합니다.** 그리고 사람은 코드보다 주석을 먼저 읽습니다. 이 주석을 믿고 "같은 상품은 한 줄일 것"이라고 가정한 코드를 누군가 작성하면 그때 버그가 생깁니다. 주석을 코드에 맞추거나 코드를 주석에 맞추거나, 둘 중 하나는 반드시 해야 합니다.

---

### [Major] MJ-8. 사용자 입력 오류에 SQLException을 사용합니다

**위치**: Service 계층 전반 — `ProductService` 6곳, `OrderItemService` 5곳, `PurchaseService` 5곳, `AuthService` 3곳

```java
// ProductService.java:20
throw new SQLException("상품명, 가격, 바코드는 필수 입력 항목입니다.");
// PurchaseService.java:37
throw new SQLException("발주 신청 수량은 한번에 최대 20개까지만 가능합니다.");
// AuthService.java:18
throw new SQLException("관리자 id와 비밀번호를 입력하세요");
```

**무엇이 문제인가**

`SQLException` 은 "DB가 이 작업을 거절했다"는 뜻입니다. "발주 수량은 최대 20개" 는 DB에 가 보지도 않은 단계의 업무 규칙입니다. 이름이 사실과 다릅니다.

**어떤 상황에서 문제가 되는가**

1. **구분 불가** — 화면의 `catch (SQLException e)` 가 "수량을 확인하세요" 와 "DB 연결 실패" 를 똑같이 처리합니다. 전자는 사용자가 다시 입력하면 되고 후자는 관리자를 불러야 하는데, 화면에서는 둘 다 "오류: ..." 로만 보입니다.
2. **전파** — `SQLException` 은 checked exception이라 호출하는 모든 메서드가 `throws SQLException` 을 달아야 합니다. 실제로 View 메서드 대부분이 이를 달고 있어, DB와 무관한 화면 코드에까지 DB 예외가 번졌습니다.
3. **예외를 다시 감싸며 원인이 사라짐** — `PurchaseService.java:51` 의 `throw new SQLException(e.getMessage())` 는 메시지만 옮기고 원래 예외를 버립니다. 스택 트레이스가 끊겨 어디서 터졌는지 추적할 수 없습니다.

**수정안**

```java
// 입력 검증 실패
throw new IllegalArgumentException("발주 신청 수량은 한번에 최대 20개까지만 가능합니다.");

// 업무 규칙 위반 — 직접 만드는 편이 더 명확하다
public class StoreException extends RuntimeException {
    public StoreException(String message) { super(message); }
}

// 예외를 감쌀 때는 원인을 반드시 넘긴다
throw new SQLException("발주 처리 실패", e);   // e 를 두 번째 인자로
```

**왜 그렇게 고치는가**

예외 타입은 **"누가 잘못했는가"** 를 알려주는 정보입니다. `IllegalArgumentException` 은 부른 쪽의 입력이 틀렸다는 뜻이고, `SQLException` 은 DB가 거절했다는 뜻입니다. 타입을 사실대로 붙이면 화면 코드가 `catch` 만 보고 "다시 입력받기" 와 "관리자 호출" 을 나눌 수 있습니다. 덤으로 `throws SQLException` 선언이 화면 계층에서 사라집니다.

---

### [Major] MJ-9. DAO와 Service가 화면에 직접 출력합니다

**위치**: `ProductDAO` 4곳, `PurchaseDAO` 3곳, `PurchaseService` 2곳, `OrderService` 1곳, `StoreInfoService` 1곳

```java
// ProductDAO.java:49
System.out.println(rows + "행이 추가되었습니다.");
// PurchaseDAO.java:124-127 — DAO 가 화면 서식까지 만든다
System.out.printf("발주 상품ID : %d , 발주 상품명 : %s , 발주 상품수량 : %d , 발주 상품 총 가격 : %d\n", ...);
// PurchaseService.java:75
System.out.println("발주목록에서 삭제 되었습니다. 발주취소 되었습니다. ---  발주ID : " + id);
```

**무엇이 문제인가**

DAO는 DB와 대화하는 계층이고 Service는 업무 규칙을 담는 계층입니다. 둘 다 "사용자가 지금 콘솔 앞에 앉아 있다"는 사실을 알면 안 됩니다.

**어떤 상황에서 문제가 되는가**

이 프로젝트는 **화면이 두 벌(콘솔, Swing)** 입니다. Swing으로 상품을 등록하면 콘솔 창에 "1행이 추가되었습니다" 가 출력되지만 사용자는 그 창을 보고 있지 않습니다. 반대로 콘솔에서는 DAO의 메시지와 View의 메시지가 둘 다 나와 같은 내용이 두 번 찍힙니다.

BL-2에서 본 "발주목록에서 삭제 되었습니다" 도 이 문제와 얽혀 있습니다. **커밋되기 전에 성공 메시지를 출력**하기 때문에, 실제로는 실패했는데 성공 메시지만 남습니다.

**수정안**: DAO와 Service에서 `System.out` 을 전부 제거하고 결과는 반환값으로 올립니다. 기록이 필요하면 의존성에 이미 들어 있는 SLF4J를 씁니다.

```java
private static final Logger log = LoggerFactory.getLogger(ProductDAO.class);
log.debug("상품 등록 완료. rows={}", rows);
```

**왜 그렇게 고치는가**: 계층을 나눈 이유는 화면을 바꿔도 아래가 그대로 재사용되게 하기 위해서입니다. 화면이 두 개인 이 프로젝트는 그 이점을 가장 크게 볼 수 있는 구조인데, 아래 계층이 콘솔에 직접 출력하는 순간 절반이 사라집니다.

---

### [Major] MJ-10. 테스트가 한 건도 없습니다

**위치**: `src/` — `main` 만 있고 `test` 디렉터리가 없습니다.

`build.gradle` 에는 JUnit 5 설정이 갖춰져 있습니다.

```gradle
testImplementation platform('org.junit:junit-bom:6.0.0')
testImplementation 'org.junit.jupiter:junit-jupiter'
test { useJUnitPlatform() }
```

**무엇이 문제인가**

준비는 되어 있는데 테스트를 한 건도 쓰지 않았습니다. 이번에 찾은 Blocker 4건은 모두 **기능당 테스트 하나면 걸렸을** 성격입니다.

| 결함 | 걸렸을 테스트 |
| :--- | :--- |
| BL-1 로그아웃 후 권한 유지 | 로그인 → 로그아웃 → 관리자 메뉴 접근이 거부되는지 |
| BL-2 발주 취소 부분 커밋 | 재고보다 큰 발주를 취소하면 예외가 나고 발주가 남는지 |
| BL-3 발주 조회 오답 | 상품 3번을 조회하면 상품 3번의 발주가 나오는지 |
| BL-4 0수량 주문 | 수량 0인 항목으로 주문하면 실패하는지 |

**수정안**: DB 없이 돌아가는 검증 테스트부터 시작하는 것이 가장 쉽습니다.

```java
class PurchaseServiceTest {
    @Test
    void 발주수량은_20개를_넘을_수_없다() {
        PurchaseService service = new PurchaseService();
        assertThrows(SQLException.class, () -> service.purcahseProduct(1, 21));
    }
    @Test
    void 발주수량은_0이하일_수_없다() {
        PurchaseService service = new PurchaseService();
        assertThrows(SQLException.class, () -> service.purcahseProduct(1, 0));
    }
}
```

이런 테스트를 쓰려면 Service가 DAO를 **밖에서 받아야** 합니다. 지금은 모든 Service가 필드에서 `new PurchaseDAO()` 로 직접 만들기 때문에 가짜 DAO로 바꿔 끼울 수 없습니다.

```java
public class PurchaseService {
    private final PurchaseDAO purchaseDAO;
    private final ProductDAO productDAO;

    public PurchaseService() { this(new PurchaseDAO(), new ProductDAO()); }
    public PurchaseService(PurchaseDAO purchaseDAO, ProductDAO productDAO) {
        this.purchaseDAO = purchaseDAO;
        this.productDAO = productDAO;
    }
```

**왜 그렇게 고치는가**: 필요한 것을 밖에서 받으면 테스트에서 가짜로 바꿔 끼울 수 있고, 안에서 만들면 못 바꿉니다. 이것이 생성자 주입을 쓰는 실질적인 이유입니다. 테스트를 한 벌이라도 돌리기 시작하면 "수정하고 나서 다시 돌려본다"는 습관이 붙고, 그게 이번 Blocker들을 막아 줍니다.

---

### [Minor] MN-1. 커넥션을 닫는 방식이 네 가지입니다

| 방식 | 위치 |
| :--- | :--- |
| try-with-resources (권장 형태) | `AdminDAO` 전체, `ProductDAO` 조회부, `OrderItemDAO` 조회부 |
| 수동 `try/catch/finally` (트랜잭션이라 불가피) | `OrderService:28-93`, `PurchaseService:41-60, 66-90, 99-125` |
| try-with-resources **안에서 또** `close()` 호출 | `OrderItemService:41 + :88`, `:103 + :142` |
| Connection을 인자로 받기 (권장) | `OrderDAO`, `OrderItemDAO` 트랜잭션용, `ProductDAO` 트랜잭션용 |

```java
// OrderItemService.java:41, 84-91 — 이중 닫기
try (Connection conn = util.getConnection()) {   // ← 여기서 자동으로 닫힌다
    ...
    } finally {
        if (conn != null) {
            conn.setAutoCommit(true);
            conn.close();                         // ← 여기서 한 번 더 닫는다
        }
    }
}
```

**무엇이 문제인가**: HikariCP의 커넥션은 두 번 닫아도 두 번째가 무시되므로 지금 당장 깨지지는 않습니다. 다만 try-with-resources의 존재 이유가 "직접 닫지 않아도 된다"인데 직접 닫고 있어, 읽는 사람이 "왜 두 번 닫지?" 를 고민하게 됩니다.

**수정안**: `finally` 블록에서는 `setAutoCommit(true)` 만 남기고 `close()` 를 지웁니다.

**왜 그렇게 고치는가**: 네 가지 방식이 한 저장소에 공존하면 새로 합류한 사람이 어느 방식을 따라야 할지 알 수 없고, 리뷰 때마다 같은 논의를 반복합니다. 조회는 try-with-resources, 트랜잭션은 Service에서 열고 DAO에 넘기기 — 두 가지로 정리하면 충분합니다.

---

### [Minor] MN-2. 예외를 삼키고 실패를 0으로 보고합니다

**위치**: `dao/AdminDAO.java:57-60`

```java
} catch (SQLException e) {
    e.printStackTrace();
    return 0;
}
```

`AuthService.registrationAdmin:48` 은 `result == 0` 을 보고 "관리자 등록에 실패했습니다" 라고만 알립니다. 아이디 중복인지, DB 장애인지, 컬럼 길이 초과인지 구분되지 않습니다. 실제 원인은 콘솔 스택 트레이스에만 남습니다.

같은 파일의 다른 두 메서드(`findByLogin:35`, `findAll:85`)는 `throw new RuntimeException(e)` 로 올려보냅니다. **한 파일 안에서 세 메서드가 두 가지 방식을 씁니다.**

**수정안**: `insertAdmin` 도 예외를 올려보내고, 중복 아이디는 `SQLIntegrityConstraintViolationException` 을 구분해 메시지를 만듭니다.

**왜 그렇게 고치는가**: 예외를 잡는 목적은 "여기서 처리할 수 있을 때"뿐입니다. DAO는 화면을 모르므로 사용자에게 무엇을 보여줄지 정할 수 없습니다. 판단할 수 없으면 올려보내야 합니다.

---

### [Minor] MN-3. 검증 메시지를 출력하고도 그대로 진행합니다

**위치**: `Service/StoreInfoService.java:16-20`

```java
public List<StoreInfo> LocationStore(String location){
    if(location == null || location.trim().isEmpty()){
        System.out.println("찾으실 매장 위치를 입력해주세요.");   // 출력만 하고
    }
    return storeInfoDAO.LocationStore(location);                 // 그대로 조회한다
}
```

빈 문자열로 조회해 결과가 0건 나오고, 화면에는 표 머리글만 출력됩니다. 사용자는 "입력해주세요" 와 빈 표를 동시에 봅니다.

**수정안**

```java
if (location == null || location.trim().isEmpty()) {
    throw new IllegalArgumentException("찾으실 매장 위치를 입력해주세요.");
}
return storeInfoDAO.LocationStore(location.trim());
```

**왜 그렇게 고치는가**: 검증은 **흐름을 멈추기 위해** 하는 것입니다. 멈추지 않는 검증은 검증이 아니라 안내문이고, 안내문이라면 화면이 낼 일입니다.

---

### [Minor] MN-4. 일반 SELECT에 `prepareCall` 을 썼습니다

**위치**: `dao/PurchaseDAO.java:120-127`

```java
try (PreparedStatement totalPstmt = conn.prepareCall(totalSql)) {
    totalPstmt.setInt(1, id);
    ResultSet rs = totalPstmt.executeQuery();   // ← ResultSet 이 try 밖
```

`prepareCall` 은 저장 프로시저를 부를 때 쓰는 `CallableStatement` 를 만듭니다. 일반 SELECT는 `prepareStatement` 입니다. MySQL에서는 동작하지만 의도와 다른 API이고, `ResultSet` 도 try-with-resources 밖에 있어 닫히지 않습니다.

**수정안**: `prepareStatement` 로 바꾸고 `ResultSet` 을 try 괄호 안으로 넣습니다.

**왜 그렇게 고치는가**: IDE 자동완성에서 비슷한 이름을 잘못 고르기 쉬운 자리입니다. 우연히 동작한다고 두면 다음 사람이 "여긴 프로시저를 쓰나?" 하고 헤맵니다.

---

### [Minor] MN-5. `nextInt()` 와 `nextLine()` 을 섞어 써서 오류 메시지가 `null` 로 나옵니다

**위치**: `view/PurchaseView.java:36, 43, 45, 52, 59, 61`

```java
case 3:
    System.out.print("발주 신청할 상품 ID : ");
    int addId = scanner.nextInt();       // 숫자가 아니면 InputMismatchException
    ...
} catch(Exception e){
    System.out.println(e.getMessage());  // InputMismatchException 은 메시지가 없다 → "null"
}
```

상품 ID에 문자를 입력하면 화면에 **`null`** 만 찍힙니다. 무엇이 잘못됐는지 알 수 없습니다.

**수정안**: 다른 View들이 쓰는 `readInt()` 방식으로 통일합니다.

```java
private int readInt(String prompt) {
    while (true) {
        System.out.print(prompt);
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("숫자를 입력해주세요.");
        }
    }
}
```

`AdminView`, `OrderItemView`, `StoreInfoView`, `Main2` 가 이미 같은 형태의 `readInt` 를 갖고 있습니다. **네 곳에 같은 메서드가 복사되어 있으니**, 공용 입력 유틸 클래스로 한 번만 두는 것이 더 낫습니다.

**왜 그렇게 고치는가**: `nextInt()` 는 줄바꿈 문자를 남겨 다음 `nextLine()` 이 빈 문자열을 읽게 만드는 문제로 유명합니다. **입력을 항상 `nextLine()` 으로 한 줄 통째로 읽고 자바에서 변환하면** 이 부류의 문제가 전부 사라집니다.

---

### [Minor] MN-6. 실행 진입점과 IDE 설정이 저장소에 섞여 있습니다

| 항목 | 위치 | 내용 |
| :--- | :--- | :--- |
| 개발 중 진입점 잔존 | `view/PurchaseView.java:95-97` | `public static void main` 이 남아 있습니다. 혼자 테스트하려고 만든 것으로 보이며, 진입점이 `Main`, `Main2`, `PurchaseView` 셋이 됩니다 |
| IDE 설정 추적 | `.idea/` 8개 파일 | `.gitignore` 가 `.idea/` 전체가 아니라 일부 파일만 제외하고 있어 `misc.xml`, `encodings.xml`, `sqldialects.xml` 등이 커밋되어 있습니다. 사람마다 달라지는 파일이라 머지 충돌의 단골입니다 |
| SQL 파일 위치 | `src/main/java/com/tenco/convenienceStore.sql` | 자바 소스 폴더 안에 SQL이 있습니다. `db/schema.sql` 처럼 별도 폴더가 찾기 쉽습니다 |

**수정안**

```gitignore
# .idea 전체를 제외
.idea/
```

```bash
git rm -r --cached .idea      # 추적 해제 (로컬 파일은 남음)
git mv src/main/java/com/tenco/convenienceStore.sql db/schema.sql
```

**왜 그렇게 고치는가**: 저장소에는 **팀이 공유해야 하는 것만** 들어가야 합니다. IDE 설정은 개인 환경이고, 빌드 산출물은 다시 만들면 되며, 실행 진입점은 하나여야 어디서 시작하는지 헷갈리지 않습니다.

---

### [Nit] 그 외

| 항목 | 위치 | 내용 |
| :--- | :--- | :--- |
| 클래스명이 소문자 | `util/util.java:9` | 자바 클래스는 대문자로 시작합니다. `DatabaseUtil` 처럼 역할이 드러나는 이름이 더 좋습니다 |
| 패키지명이 대문자 | `com.tenco.Service` | 패키지는 전부 소문자입니다. 다른 패키지(`dao`, `dto`, `view`)는 맞습니다 |
| DTO 필드 오타 | `dto/PurchaseDto.java:13, 15` | `qauntity`(→quantity), `totlaPrice`(→totalPrice). 호출부 전체에 오타가 퍼져 있습니다 |
| 메서드명 오타 | `Service/PurchaseService.java:34, 94` | `purcahseProduct`(→purchase), `substractPurchase`(→subtract) |
| 메서드명이 대문자 시작 | `AdminView:20`, `StoreInfoService:16` | `AdminStart`, `LocationStore`. 메서드는 소문자로 시작합니다 |
| 필드명이 대문자 시작 | `dto/StoreInfo.java:20` | `private String Location;` |
| 정체불명 메서드 | `dto/PurchaseDto.java:20` | `toString3()`. `@Data` 가 이미 `toString()` 을 만들어 줍니다 |
| Lombok 애너테이션 불일치 | DTO 6개 | `@Data` 3개 vs `@Getter`+`@Setter` 3개가 섞여 있습니다 |
| 미사용 import | `view/StoreInfoView.java:5`, `view/ProductView.java:5` | `com.tenco.util.util` 을 쓰지 않습니다 |
| 잘못된 안내 문구 | `view/PurchaseView.java:19, 70` | 메뉴는 1~6인데 오류 메시지는 "메뉴는 1 ~ 5번에서 골라주세요" |
| 오타 | `view/PurchaseView.java:44` | "발주할 수랑" → "수량" |
| 잘못된 오류 메시지 | `Service/PurchaseService.java:112` | 재고를 **차감**하는 코드인데 실패 메시지가 "상품 재고 증가 실패" |
| 죽은 필드 | `dto/Order.java:19` | `orders` 테이블에 `status` 컬럼이 없어 항상 `null` 입니다 (확인 완료) |
| 검증 순서가 거꾸로 | `Service/PurchaseService.java:24-28` | DAO를 먼저 호출하고 그 뒤에 `id <= 0` 을 검사합니다 |
| DTO를 부분만 채움 | `dao/ProductDAO.java:58-78` | `getProductName()` 은 `Product` 객체에 이름만 넣습니다. 나머지 필드는 0/null이라 받는 쪽이 오해하기 쉽습니다 |
| 화면 중복 | `view/` 6개 vs `swing/` 6개 | 같은 기능이 두 벌입니다. 리뷰 범위 밖이지만, 한쪽만 고치는 상황이 생기기 쉽습니다 |

---

## 5. 담당 기능별 정리

### 관리자 / 로그인

| 항목 | 상태 |
| :--- | :--- |
| 잘된 점 | BCrypt 해시 검증, 로그인 결과와 목록 조회 양쪽에서 비밀번호 제거, `login_id` UNIQUE 제약, 등록 전 중복 확인 |
| 고칠 점 | **로그아웃해도 권한이 유지됨 (BL-1)**, `insertAdmin` 의 예외 삼킴 (MN-2), 검증에 `SQLException` 사용 (MJ-8) |
| 검증 결과 | `admin01/1234` 정상 로그인, 틀린 비밀번호 정상 거부 확인 |

### 상품

| 항목 | 상태 |
| :--- | :--- |
| 잘된 점 | `outStock` 의 조건부 UPDATE (3-2), 매핑 메서드 추출(`createProduct`), 트랜잭션용 메서드가 `Connection` 을 받음, 재고 부족 기준(10개)을 SQL로 처리 |
| 고칠 점 | 유통기한 NULL 처리 불일치로 전체 조회가 NPE (MJ-2), `status` 복구 불가 (MJ-3), 삭제 실패가 View를 뚫고 나감 (MJ-5), DAO의 화면 출력 4곳 (MJ-9) |
| 눈에 띄는 구조 | 같은 파일 안에서 `createProduct` 와 `selectProductById` 가 같은 컬럼을 다르게 다룹니다. 한 명이 조회 기능을, 다른 한 명이 트랜잭션 기능을 맡으면서 갈린 것으로 보입니다. **어느 쪽이 맞는지 정하고 하나로 합치면 됩니다** |

### 주문 / 결제

| 항목 | 상태 |
| :--- | :--- |
| 잘된 점 | 트랜잭션 경계가 Service (3-1), 장바구니의 실시간 재고 반영 (3-6), 취소 시 재고 복구 → order_item 삭제 → orders 삭제 순서가 FK 제약과 맞음 |
| 고칠 점 | **수량 0 입력이 0원 주문 생성 (BL-4)**, 재고 음수 방지 누락 (MJ-1), 중복 상품 행 동시 변경 (MJ-4), 쓰이지 않는 두 번째 취소 경로 (MJ-6) |
| 검증 결과 | 정상 주문 흐름은 재고 차감과 order_item 생성이 모두 맞게 동작합니다. 문제는 전부 **예외 흐름**(0 입력, 재고 초과, 중복 행)에 있습니다 |

### 주문 상품 (OrderItem)

| 항목 | 상태 |
| :--- | :--- |
| 잘된 점 | **이 프로젝트에서 트랜잭션을 가장 정확하게 쓴 영역입니다.** 수량 증감에 따라 `outStock`/`inStock` 을 나눠 부르고, 모든 DAO 호출의 반환값을 확인하며, 마지막 상품이 삭제되면 주문까지 삭제합니다. `COALESCE(SUM(...), 0)` 으로 NULL 합계도 처리했습니다 |
| 고칠 점 | 커넥션 이중 닫기 (MN-1), 검증에 `SQLException` 사용 (MJ-8) |
| 판단 | 다른 영역에서 빠진 "반환값 확인"을 여기서는 빠짐없이 했습니다. **BL-2를 고칠 때 이 파일을 본보기로 삼으면 됩니다** |

### 발주 / 입고

| 항목 | 상태 |
| :--- | :--- |
| 잘된 점 | 발주 신청이 `purchase` 등록과 `product` 재고 증가를 하나의 트랜잭션으로 묶음, 발주 수량 상한(20개) 검증, 수량 차감 SQL에 `and quantity - ? >= 0` 가드 |
| 고칠 점 | **취소 시 부분 커밋 (BL-2)**, **단건 조회가 엉뚱한 상품 반환 (BL-3)**, 주석과 코드 불일치 (MJ-7), DAO의 화면 출력 3곳 (MJ-9), `prepareCall` 오용 (MN-4), 입력 처리 (MN-5) |
| 판단 | 트랜잭션을 쓰겠다는 판단은 맞았는데 **예외 타입 하나(`RuntimeException` vs `SQLException`)를 잘못 골라 트랜잭션이 거꾸로 동작합니다.** 고칠 양은 적지만 영향은 큽니다. `substractPurchase` 의 `and quantity - ? >= 0` 가드는 `outStock` 과 같은 발상이라 좋습니다 |

### 매장 정보

| 항목 | 상태 |
| :--- | :--- |
| 잘된 점 | 가장 단순하고 읽기 쉬운 코드입니다. 조회 전용이라 트랜잭션이 필요 없고, try-with-resources가 3중으로 정확히 걸려 있으며, 매핑 메서드도 분리되어 있습니다 |
| 고칠 점 | 검증이 흐름을 멈추지 않음 (MN-3), 메서드/필드 명명 (Nit), 미사용 import |

### 공통 / 콘솔 화면

| 항목 | 상태 |
| :--- | :--- |
| 잘된 점 | HikariCP 설정에 주석으로 각 옵션의 의미를 적어 둠, `close()` 메서드 제공, 메뉴별 클래스 분리 |
| 고칠 점 | `util.close()` 를 아무도 호출하지 않음, `readInt` 가 네 클래스에 복사됨 (MN-5), `Main2` 의 권한 관리 (BL-1) |

---

## 6. 코드 컨벤션과 Git 관리

### 6-1. 팀 규칙이 없어서 갈린 지점

아래는 개인의 실수가 아니라 **팀이 정하지 않아서 생긴 차이**입니다. 규칙을 하나 정하면 전부 사라집니다.

| 항목 | 갈린 방식 | 위치 |
| :--- | :--- | :--- |
| 커넥션 관리 | 4가지 | MN-1 표 참조 |
| 재고 차감 SQL | 가드 있음 vs 없음 | `ProductDAO:315` vs `OrderDAO:109` |
| 예외 타입 | `RuntimeException` / `SQLException` / 삼키고 0 반환 | DAO 6개 |
| 예외 감싸기 | 원인 전달 vs 메시지만 복사 | `PurchaseService:56` vs 나머지 |
| 검증 실패 | `SQLException` 19곳 vs `System.out` 1곳 | Service 6개 |
| DAO 주입 | 전부 필드에서 `new` | Service 6개 (테스트 불가 원인, MJ-10) |
| NULL 날짜 처리 | 검사함 vs 안 함 | `ProductDAO:299` vs `:271` |
| 반환값 확인 | 전부 확인 vs 무시 | `OrderItemService` vs `PurchaseService:73` |
| Lombok | `@Data` vs `@Getter`+`@Setter` | DTO 6개 |
| 입력 읽기 | `readInt()` vs `scanner.nextInt()` | View 4개 vs `PurchaseView` |
| SQL 키워드 | 대문자 vs 소문자 | `ProductDAO`/`OrderDAO` vs `PurchaseDAO`/`StoreInfoDAO` |
| 네이밍 | `get`/`find`/`select`/`search` 혼용 | DAO 6개 |

**가장 값이 큰 규칙 하나를 고른다면 "DAO 메서드의 반환값은 반드시 확인한다"** 입니다. BL-2가 정확히 이것을 지키지 않아 생겼고, `OrderItemService` 는 지켜서 안전합니다.

### 6-2. Git 이력

실제로 확인한 수치입니다.

| 항목 | 수치 | 판단 |
| :--- | :--- | :--- |
| 전체 커밋 | 155 | 7일간(09-11 ~ 09-17). 활동량 충분 |
| PR 머지 | 21건 (PR 번호 #30까지) | **좋음** |
| 머지 커밋 | 43 (27.7%) | 보통 |
| GitHub 웹에서 만든 커밋 | 42 (27.1%) | 과다 |
| 저자 식별자 | 9종 (실제 4~5명) | 정리 필요 |

**(1) 잘한 점 — PR 기반 협업**

`Merge pull request #30 from DevDash-boot/purchase33` 같은 커밋이 21건입니다. 기능 브랜치(`purchase33`, `store7`, `feature/add-order`, `yy` 등)를 만들고 PR로 합쳤습니다. 이전 단계에서 흔한 "전원이 main에 직접 커밋" 을 벗어났습니다. PR 번호가 #30까지 올라간 것을 보면 닫히거나 병합되지 않은 PR도 있었다는 뜻인데, 그것도 정상적인 협업의 모습입니다.

**(2) 저자 식별자가 9종입니다**

```
100  DevDash      <jeonghs1570@gmail.com>
     DebDash-boot <jeonghs1570@gmail.com>   ← 같은 이메일, 이름에 오타 (Dev→Deb)
 36  yuyn2000     <yuyn2000@naver.com>
     MIKY-0       <yuyn2000@naver.com>      ← 같은 이메일
  1  MIKY-1       <yuyn2000@gmail.com>      ← 같은 사람, 다른 이메일
  6  윤아          <yunah051011@gmail.com>
  6  yunah111111  <...@users.noreply.github.com>   ← GitHub 웹 편집
  4  박준형        <bito4165@gmail.com>
  2  bito4165     <bito4165@naver.com>      ← 같은 사람, 다른 이메일
```

한 사람이 집과 학원에서 `user.name` 을 다르게 설정하거나 GitHub 웹 편집기를 섞어 쓰면 이렇게 갈립니다. 누구의 잘못도 아니고, 팀에서 한 번 맞추지 않으면 반드시 생기는 현상입니다. `DebDash-boot` 는 `DevDash-boot` 의 오타로 보입니다.

**실무였다면 무엇이 문제인가**: GitHub 기여도 그래프가 사람 단위로 집계되지 않고, `git blame` 으로 작성자를 찾을 때 헷갈립니다. 회사에서는 사내 이메일 외의 커밋을 CI가 거부하도록 설정하는 경우도 있습니다.

**개선**: 프로젝트 시작 시 각자 한 번 설정합니다.

```bash
git config user.name "본인이 정한 표기 하나"
git config user.email "GitHub 계정에 등록된 이메일"
```

이미 갈라진 이력은 저장소 루트에 `.mailmap` 을 두면 통계에서만 합칠 수 있습니다.

```
# .mailmap — 왼쪽이 대표 표기, 오른쪽이 합칠 대상
DevDash-boot <jeonghs1570@gmail.com> DebDash-boot <jeonghs1570@gmail.com>
```

**(3) GitHub 웹에서 만든 커밋이 27%입니다**

`Update ProductDAO.java`, `Update Main.java`, `Add files via upload`, `Delete src/main/java/com/tenco/ProductTest.java` 같은 커밋이 42건입니다. 이 메시지는 GitHub가 자동으로 붙이는 것이라 **무엇을 왜 바꿨는지 전혀 알려주지 않습니다.**

**실무였다면 무엇이 문제인가**: 웹 편집은 컴파일도 실행도 해 보지 않고 커밋하는 것입니다. 이번에 찾은 Blocker 중 BL-2(발주 취소)가 들어 있는 `PurchaseService.java` 는 `Update PurchaseService.java` 로 3번 커밋되었습니다. 브라우저에서 고치면 예외 타입이 맞는지 IDE가 알려주지 않습니다.

**개선**: 로컬에서 고치고, 실행해 보고, 커밋합니다. 웹 편집은 README의 오타 수정 정도로 제한합니다.

**(4) 커밋 메시지**

```
11  /  기능  /  마무리  /  수정완료  /  main pull 전
콘솔창 실행 완료   (5회 반복)
Merge branch 'main' of ... into store6 # Please enter a commit message to explain why this merge is necessary, # especially if it merges an updated upstream into a topic branch. ...
```

마지막 것은 머지 커밋 편집기의 **안내문이 그대로 커밋 메시지가 된 경우**입니다(`f4957a6`). 편집기에서 `#` 로 시작하는 줄이 주석인데, 한 줄로 합쳐지면서 그대로 저장되었습니다.

반면 잘 쓴 것도 있습니다.

```
관리자 스윙 로그인 풀림 문제 수정
발주 시 총금액 수정
트랜잭션 finally 추가
```

**개선**: `[영역] 무엇을 왜` 한 줄이면 충분합니다.

```
발주: 취소 시 재고 차감 실패를 확인하도록 수정 (부분 커밋되던 문제)
주문: 수량 0 입력 시 장바구니에 담기지 않도록 수정
```

**(5) 브랜치 이름**

```
purchase33   store7   yy   add_order   feature/add-order   feature/김희성관리자테이블   DevDash-boot-patch-1
```

접두어 방식이 네 가지입니다. `purchase33`, `store7`, `yy` 는 숫자나 약자가 붙어 무엇을 하는 브랜치인지 알 수 없고, `feature/김희성관리자테이블` 은 사람 이름이 들어가 있으며, `DevDash-boot-patch-1` 은 GitHub 웹 편집이 자동 생성한 이름입니다.

**개선**: `feature/기능명` 하나로 통일합니다. 사람 이름은 커밋 저자로 이미 남으므로 브랜치 이름에 넣지 않습니다.

**(6) 커밋 집중도**

이메일 기준으로 합치면 최다 기여자 한 명이 100커밋(65%)입니다. 커밋 수가 곧 기여도는 아니고, 통합 작업은 원래 커밋이 잘게 쪼개진다는 점은 감안해야 합니다.

**실무였다면 무엇이 문제인가**: 개인의 문제가 아니라 **분담 방식의 문제**입니다. 통합을 한 사람이 전담하면 그 사람이 빠질 때 프로젝트가 멈추고, 나머지 팀원은 자기 기능이 화면에서 어떻게 쓰이는지 경험하지 못한 채 끝납니다.

**개선**: 통합본이 나온 뒤 담당자별로 자기 메뉴를 한 번씩 눌러 보는 절차를 넣습니다. 30분이면 끝나고, 이번 Blocker 4건은 거기서 전부 걸렸을 것입니다.

**(7) 그 밖에 잘한 점**

- `gradle-wrapper.jar` 가 커밋되어 있어 clone 후 바로 빌드됩니다.
- **비밀번호를 하드코딩한 커밋이 이력 전체에 한 건도 없습니다.** 첫 커밋부터 `System.getenv` 였습니다.
- README에 기능별 점검 항목(`● 발주 신청 시 product 의 재고가 증가하는지 확인` 등)을 적어 두었습니다. **이것이 사실상 테스트 시나리오입니다.** 이대로 JUnit 테스트로 옮기면 MJ-10이 바로 해결됩니다.

---

## 7. 다음 프로젝트를 위한 체크리스트

### 시작 전 30분 회의에서 정할 것

- [ ] **DAO 메서드의 반환값은 반드시 확인한다** (이번 Blocker의 최대 원인)
- [ ] 조건부 UPDATE(`WHERE ... AND stock >= ?`)를 쓸 곳을 정하고, 모든 재고 변경이 그 메서드를 거치게 한다
- [ ] 예외 — 입력 검증 실패는 `IllegalArgumentException`, DB 오류는 그대로 올리기
- [ ] 예외를 감쌀 때는 **원인 예외를 두 번째 인자로 반드시 넘긴다**
- [ ] 커넥션 — 조회는 try-with-resources, 트랜잭션은 Service에서 열고 DAO에 넘기기
- [ ] 입력은 항상 `nextLine()` 으로 읽고 자바에서 변환한다 (`nextInt()` 금지)
- [ ] 화면 출력은 View에서만. DAO/Service에 `System.out` 금지
- [ ] DAO 주입 — 생성자 주입으로 통일 (테스트를 위해)
- [ ] SQL 키워드 대소문자, DAO 메서드 접두어(`find`/`get`/`select`) 중 하나
- [ ] 브랜치 이름 `feature/기능명`, 커밋 메시지 `[영역] 무엇을 왜`
- [ ] `git config user.name` / `user.email` 전원 통일

### 저장소에 반드시 들어가야 할 것

- [x] `db/schema.sql` — **이미 있습니다.** 위치만 `src/main/java` 밖으로 옮기면 됩니다
- [x] `gradle/wrapper/gradle-wrapper.jar` — **이미 있습니다**
- [ ] README에 "clone 후 실행하는 법" 3~5줄 (환경변수 `DB_USER`/`DB_PASSWORD` 설정 포함)
- [ ] `src/test` — 기능당 최소 1개

### 저장소에 들어가면 안 되는 것

- [x] 비밀번호, API 키 — **이번 프로젝트는 지켰습니다. 계속 유지하십시오**
- [ ] IDE 설정(`.idea/`) — 현재 8개 파일이 추적 중입니다
- [ ] 개발 중 만든 진입점(`PurchaseView.main`)

### 기능을 "완료" 라고 부르기 전에

- [ ] 그 메뉴를 실제로 눌러서 결과 화면을 봤는가
- [ ] **정상 흐름뿐 아니라 예외 흐름도 눌러봤는가** — 0 입력, 문자 입력, 없는 번호, 재고보다 큰 수량, 로그아웃 후 접근
- [ ] DB를 직접 열어 의도한 대로 들어갔는지 확인했는가
- [ ] 트랜잭션을 썼다면, **실패하는 경우를 일부러 만들어서** 롤백되는지 확인했는가

> 이번 Blocker 4건은 모두 마지막 두 항목에서 걸렸을 문제입니다. 정상 흐름은 네 기능 모두 잘 동작합니다. **문제는 전부 "사용자가 취소하거나, 값이 모자라거나, 로그아웃할 때"에 있습니다.**

### 다음 단계에서 시도해 볼 것

- [ ] README의 점검 항목(`● ...`)을 JUnit 테스트로 옮기기 — 이미 시나리오가 다 적혀 있습니다
- [ ] 파생 데이터(`product.status`)를 저장하지 않고 조회 시 계산하기 (MJ-3)
- [ ] 화면이 두 벌(콘솔/Swing)인 상태를 정리하기 — 검증 로직을 Service로 모으면 두 화면이 같은 규칙을 공유합니다
- [ ] `purchase` 테이블에 발주 일시 컬럼 추가하기

---

## 부록 A. 검증 환경과 스키마 사실

### 검증 환경을 그대로 재현하는 방법

```bash
# 1. 저장소에 있는 스키마로 DB 생성
mysql -u root -p < src/main/java/com/tenco/convenienceStore.sql

# 2. 환경변수 설정 후 실행
set DB_USER=root
set DB_PASSWORD=root
gradlew run
```

`util.java:10` 의 URL이 `192.168.5.4` 로 고정되어 있으므로, 로컬에서 돌리려면 이 값을 바꿔야 합니다. **접속 주소도 환경변수로 빼는 것을 권합니다.**

```java
private static final String URL = System.getenv().getOrDefault(
        "DB_URL", "jdbc:mysql://192.168.5.4:3306/convenience_store?serverTimezone=Asia/Seoul");
```

### 코드를 읽을 때 같이 봐야 하는 스키마 사실

| 사실 | 코드에 미치는 영향 |
| :--- | :--- |
| `product.expiration_date` 가 NULL 허용 | MJ-2. `createProduct` 가 NPE를 낸다 |
| `order_item` 에 `(order_id, product_id)` UNIQUE 없음 | MJ-4. 수량 변경이 여러 행을 함께 바꾼다 |
| `purchase` 에 `product_id` UNIQUE 없음 | MJ-7. 같은 상품 발주가 여러 줄 쌓인다 |
| FK가 모두 `NO ACTION` | MJ-5. 주문·발주 이력이 있는 상품은 삭제 불가 |
| `orders` 에 `status` 컬럼 없음 | MJ-6, Nit. `Order.status` 는 항상 null |
| `purchase` 에 날짜 컬럼 없음 | 언제 발주한 건인지 알 수 없다 |
| `admin.login_id`, `product.barcode` 는 UNIQUE | 중복은 DB가 막아 주지만, 자바가 사전 확인을 안 해 DB 원문이 노출된다 |

### 운영 DB 점검 쿼리

이 리뷰는 초기 스키마 기준으로 검증했습니다. **운영 DB에 이미 손상된 데이터가 있는지**는 아래로 확인하십시오. 전부 조회 전용입니다.

```sql
-- 1) 재고가 음수인 상품 (MJ-1)
SELECT product_id, product_name, stock FROM product WHERE stock < 0;

-- 2) 수량이 0 이하인 주문 항목 (BL-4)
SELECT * FROM order_item WHERE quantity <= 0;

-- 3) 금액이 0 이하인 주문 (BL-4)
SELECT * FROM orders WHERE total_price <= 0;

-- 4) 재고가 있는데 판매 중지 상태인 상품 (MJ-3)
SELECT product_id, product_name, stock, status FROM product WHERE stock > 0 AND status = 0;

-- 5) 유통기한이 비어 있는 상품 (MJ-2 — 한 건만 있어도 상품 조회 전체가 멈춥니다)
SELECT product_id, product_name FROM product WHERE expiration_date IS NULL;

-- 6) 같은 주문에 같은 상품이 두 줄 이상 (MJ-4)
SELECT order_id, product_id, COUNT(*) FROM order_item
GROUP BY order_id, product_id HAVING COUNT(*) > 1;

-- 7) 주문 총액과 상세 합계가 어긋난 주문 (MJ-1, MJ-4의 흔적)
SELECT o.order_id, o.total_price AS 주문총액,
       COALESCE(SUM(oi.order_price * oi.quantity), 0) AS 상세합계
FROM orders o LEFT JOIN order_item oi ON o.order_id = oi.order_id
GROUP BY o.order_id, o.total_price
HAVING o.total_price <> COALESCE(SUM(oi.order_price * oi.quantity), 0);
```

7번이 특히 중요합니다. 주문 총액과 상세 합계가 다르다면 MJ-1이나 MJ-4를 이미 밟은 것입니다.

### 권장 스키마 보강

```sql
ALTER TABLE order_item ADD UNIQUE KEY uk_order_product (order_id, product_id);   -- MJ-4
ALTER TABLE purchase   ADD COLUMN purchase_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;  -- MJ-7
ALTER TABLE product    MODIFY expiration_date DATE NOT NULL;   -- MJ-2 (유통기한이 필수인 경우만)
```

첫 번째는 중복 행이 이미 있으면 실패하므로, 위 6번 쿼리로 먼저 확인하십시오.

---

## 부록 B. 수정 우선순위

| 순서 | 항목 | 예상 작업량 | 효과 |
| :--- | :--- | :--- | :--- |
| 1 | BL-1 `Main2` 의 `isAdmin` 을 `adminView.isLoggedIn()` 으로 교체 | 10줄 | 로그아웃이 실제로 동작 |
| 2 | BL-2 `catch (RuntimeException)` → `catch (SQLException)` + `outStock` 반환값 확인 | 5줄 | 발주 취소의 데이터 손상 차단 |
| 3 | BL-4 수량 0 입력 시 장바구니에 담지 않도록 `continue` | 5줄 | 0원 주문 생성 차단 |
| 4 | BL-3 `existList` 를 `findByPurchaseId` / `findByProductId` 로 분리 | 20줄 | 발주 조회가 맞는 데이터 반환 |
| 5 | **운영 DB 점검** (부록 A의 쿼리 7개) | 10분 | 이미 손상된 데이터 확인 |
| 6 | MJ-1 `updateOrderItemAndStock` 이 `outStock`/`inStock` 을 쓰도록 | 10줄 | 재고 음수 차단 |
| 7 | MJ-2 `createProduct` 의 NULL 검사 추가 | 3줄 | 상품 조회 멈춤 방지 |
| 8 | MJ-3 `updateStatus` 를 `SET status = (stock > 0)` 로 | 1줄 | 재고 보충 후 판매 재개 |
| 9 | MJ-4 UNIQUE 제약 추가 + UPDATE 조건을 PK로 | SQL 1줄 + 1줄 | 중복 행 차단 |
| 10 | MJ-6 쓰이지 않는 두 번째 주문취소 경로 삭제 | 삭제만 | 잘못 고를 위험 제거 |
| 11 | 나머지 Major | 별도 논의 | 구조 개선 |

1~4번은 **합쳐서 40줄 남짓**입니다. 이것만 고쳐도 "정상 메뉴를 눌렀을 때 데이터가 깨지는 경로"가 사라집니다.

5번은 코드 수정이 아니라 **현재 상태 확인**입니다. Blocker들이 이미 몇 번 실행되었을 수 있으므로, 고치기 전에 무엇이 어긋나 있는지 먼저 봐야 합니다.

---

*이 문서는 코드, 커밋 이력, 그리고 저장소의 `convenienceStore.sql` 로 만든 로컬 검증 DB에서 학생 코드를 직접 실행한 결과를 근거로 작성했습니다. 팀 운영 DB(`192.168.5.4`)에는 접속하지 않았고, 원본 소스는 수정하지 않았습니다. 남은 "확인 필요" 항목은 2장에 정리되어 있습니다.*
