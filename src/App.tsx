import { useMemo, useState } from "react";

type IconName =
  | "calendar"
  | "check"
  | "book"
  | "search"
  | "plus"
  | "chevron-left"
  | "chevron-right"
  | "bell"
  | "clock"
  | "sparkles"
  | "x"
  | "filter"
  | "more"
  | "layout"
  | "list";

const iconPaths: Record<IconName, React.ReactNode> = {
  calendar: <><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M16 3v4M8 3v4M3 10h18" /></>,
  check: <><path d="m5 12 4 4L19 6" /></>,
  book: <><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" /><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" /></>,
  search: <><circle cx="11" cy="11" r="7" /><path d="m20 20-4-4" /></>,
  plus: <><path d="M12 5v14M5 12h14" /></>,
  "chevron-left": <><path d="m15 18-6-6 6-6" /></>,
  "chevron-right": <><path d="m9 18 6-6-6-6" /></>,
  bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M13.7 21a2 2 0 0 1-3.4 0" /></>,
  clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
  sparkles: <><path d="m12 3-1.2 3.3L7.5 7.5l3.3 1.2L12 12l1.2-3.3 3.3-1.2-3.3-1.2L12 3ZM5 14l-.8 2.2L2 17l2.2.8L5 20l.8-2.2L8 17l-2.2-.8L5 14ZM18 13l-1 2.7-2.7 1 2.7 1 1 2.7 1-2.7 2.7-1-2.7-1-1-2.7Z" /></>,
  x: <><path d="m6 6 12 12M18 6 6 18" /></>,
  filter: <><path d="M4 6h16M7 12h10M10 18h4" /></>,
  more: <><circle cx="5" cy="12" r="1" fill="currentColor" stroke="none" /><circle cx="12" cy="12" r="1" fill="currentColor" stroke="none" /><circle cx="19" cy="12" r="1" fill="currentColor" stroke="none" /></>,
  layout: <><rect x="3" y="3" width="18" height="18" rx="2" /><path d="M9 3v18M9 9h12" /></>,
  list: <><path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01" /></>,
};

function Icon({ name, size = 18 }: { name: IconName; size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {iconPaths[name]}
    </svg>
  );
}

const days = [
  { day: "월", date: 17, full: "11월 17일" },
  { day: "화", date: 18, full: "11월 18일" },
  { day: "수", date: 19, full: "11월 19일", today: true },
  { day: "목", date: 20, full: "11월 20일" },
  { day: "금", date: 21, full: "11월 21일" },
  { day: "토", date: 22, full: "11월 22일", weekend: true },
  { day: "일", date: 23, full: "11월 23일", weekend: true },
];

const events = [
  { id: 1, day: 0, start: 9, duration: 1.5, title: "알고리즘 강의", meta: "09:00 – 10:30", type: "fixed", color: "blue" },
  { id: 2, day: 0, start: 14, duration: 2, title: "자료구조 과제", meta: "학습 블록 · 2시간", type: "study", color: "violet" },
  { id: 3, day: 1, start: 11, duration: 1, title: "팀 프로젝트 회의", meta: "11:00 – 12:00", type: "fixed", color: "green" },
  { id: 4, day: 1, start: 15, duration: 1.5, title: "UX 리서치 보고서", meta: "학습 블록 · 1.5시간", type: "study", color: "orange" },
  { id: 5, day: 2, start: 9, duration: 1.5, title: "소프트웨어 공학", meta: "09:00 – 10:30", type: "fixed", color: "blue" },
  { id: 6, day: 2, start: 13, duration: 2, title: "알고리즘 과제", meta: "학습 블록 · 2시간", type: "study", color: "violet" },
  { id: 7, day: 3, start: 10, duration: 1.5, title: "데이터베이스 강의", meta: "10:00 – 11:30", type: "fixed", color: "green" },
  { id: 8, day: 3, start: 16, duration: 1, title: "자료구조 복습", meta: "학습 블록 · 1시간", type: "study", color: "violet" },
  { id: 9, day: 4, start: 9.5, duration: 1, title: "지도교수 상담", meta: "09:30 – 10:30", type: "fixed", color: "orange" },
  { id: 10, day: 4, start: 13, duration: 2, title: "UX 리서치 보고서", meta: "학습 블록 · 2시간", type: "study", color: "orange" },
  { id: 11, day: 5, start: 11, duration: 2, title: "알고리즘 과제", meta: "학습 블록 · 2시간", type: "study", color: "violet" },
];

const assignments = [
  { title: "알고리즘 분석 과제", course: "알고리즘", due: "11월 21일", done: 6, total: 10, color: "violet" },
  { title: "UX 리서치 보고서", course: "소프트웨어 공학", due: "11월 24일", done: 4.5, total: 8, color: "orange" },
  { title: "DB 모델링 실습", course: "데이터베이스", due: "11월 28일", done: 2, total: 6, color: "green" },
];

function App() {
  const [view, setView] = useState<"week" | "month">("week");
  const [showForm, setShowForm] = useState(false);
  const [query, setQuery] = useState("");
  const [activeCourse, setActiveCourse] = useState("전체");
  const [toast, setToast] = useState(false);
  const [formTitle, setFormTitle] = useState("");

  const filteredEvents = useMemo(() => {
    const q = query.trim().toLowerCase();
    return events.filter((event) => {
      const courseMatches = activeCourse === "전체" || event.title.includes(activeCourse);
      const queryMatches = !q || event.title.toLowerCase().includes(q);
      return courseMatches && queryMatches;
    });
  }, [query, activeCourse]);

  const submitAssignment = (event: React.FormEvent) => {
    event.preventDefault();
    if (!formTitle.trim()) return;
    setShowForm(false);
    setToast(true);
    setFormTitle("");
    window.setTimeout(() => setToast(false), 3200);
  };

  return (
    <div className="app-shell">
      <div className="desktop-chrome">
        <div className="window-titlebar">
          <div className="window-app">
            <span className="mini-mark"><Icon name="check" size={12} /></span>
            <strong>미루지마</strong>
            <span>Enough Procrastination — 개인 일정 관리</span>
          </div>
          <div className="window-controls" aria-label="창 제어">
            <button aria-label="최소화">—</button>
            <button aria-label="최대화">□</button>
            <button aria-label="닫기">×</button>
          </div>
        </div>
        <nav className="menu-bar" aria-label="애플리케이션 메뉴">
          <button><u>파</u>일</button>
          <button><u>편</u>집</button>
          <button><u>일</u>정</button>
          <button><u>과</u>제</button>
          <button><u>보</u>기</button>
          <button><u>도</u>움말</button>
        </nav>
      </div>
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark"><Icon name="check" size={17} /></div>
          <div><strong>미루지마</strong><span>Enough Procrastination</span></div>
        </div>

        <nav className="primary-nav" aria-label="주요 메뉴">
          <button className="nav-item active"><Icon name="calendar" /><span>캘린더</span></button>
          <button className="nav-item"><Icon name="check" /><span>과제</span><em>3</em></button>
          <button className="nav-item"><Icon name="book" /><span>과목</span></button>
        </nav>

        <div className="sidebar-section">
          <div className="section-label"><span>내 과목</span><button aria-label="과목 추가"><Icon name="plus" size={15} /></button></div>
          {[
            ["알고리즘", "violet"],
            ["소프트웨어 공학", "blue"],
            ["데이터베이스", "green"],
            ["개인 일정", "orange"],
          ].map(([name, color]) => (
            <button key={name} onClick={() => setActiveCourse(activeCourse === name ? "전체" : name)} className={`course-item ${activeCourse === name ? "selected" : ""}`}>
              <i className={`dot ${color}`} /><span>{name}</span>
            </button>
          ))}
        </div>

        <div className="sidebar-tip">
          <Icon name="sparkles" size={17} />
          <div><strong>오늘도 한 블록씩</strong><p>이번 주 계획의 64%를 완료했어요.</p></div>
        </div>
        <div className="profile">
          <div className="avatar">지</div>
          <div><strong>한지수</strong><span>대학생</span></div>
          <button aria-label="프로필 메뉴"><Icon name="more" /></button>
        </div>
      </aside>

      <main className="main">
        <header className="topbar">
          <div className="search-wrap">
            <Icon name="search" size={17} />
            <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="일정이나 과제를 검색하세요" aria-label="일정 검색" />
            <kbd>⌘ K</kbd>
          </div>
          <div className="top-actions">
            <button className="icon-button" aria-label="알림"><Icon name="bell" /><i /></button>
            <button className="primary-button" onClick={() => setShowForm(true)}><Icon name="plus" size={17} />새 일정</button>
          </div>
        </header>

        <div className="content">
          <section className="welcome">
            <div>
              <p className="eyebrow">수요일, 2025년 11월 19일</p>
              <h1>좋은 아침이에요, 지수님</h1>
              <p className="subtitle">오늘은 <b>3개의 학습 블록</b>이 예정되어 있어요. 차근차근 시작해 볼까요?</p>
            </div>
            <div className="stats">
              <div><span className="stat-icon lavender"><Icon name="clock" /></span><p>오늘의 학습</p><strong>4.5<small>시간</small></strong></div>
              <div><span className="stat-icon mint"><Icon name="check" /></span><p>이번 주 완료</p><strong>64<small>%</small></strong></div>
            </div>
          </section>

          <section className="calendar-card">
            <div className="calendar-toolbar">
              <div className="date-nav">
                <h2>2025년 11월</h2>
                <button aria-label="이전 주"><Icon name="chevron-left" size={17} /></button>
                <button aria-label="다음 주"><Icon name="chevron-right" size={17} /></button>
                <button className="today-button">오늘</button>
              </div>
              <div className="view-tools">
                <button className="filter-button"><Icon name="filter" size={16} />필터</button>
                <div className="segmented">
                  <button onClick={() => setView("week")} className={view === "week" ? "active" : ""}><Icon name="layout" size={15} />주</button>
                  <button onClick={() => setView("month")} className={view === "month" ? "active" : ""}><Icon name="list" size={15} />월</button>
                </div>
              </div>
            </div>

            {view === "week" ? (
              <div className="calendar-scroll">
                <div className="week-calendar">
                  <div className="week-header">
                    <div className="timezone">GMT+9</div>
                    {days.map((item) => (
                      <div key={item.date} className={`day-head ${item.today ? "today" : ""} ${item.weekend ? "weekend" : ""}`}>
                        <span>{item.day}</span><strong>{item.date}</strong>
                      </div>
                    ))}
                  </div>
                  <div className="all-day-row">
                    <div className="all-day-label">종일</div>
                    <div /><div /><div className="deadline-chip">알고리즘 과제 마감</div><div /><div /><div /><div />
                  </div>
                  <div className="calendar-body">
                    <div className="time-column">
                      {Array.from({ length: 11 }, (_, i) => <span key={i} style={{ top: i * 72 - 8 }}>{String(i + 8).padStart(2, "0")}:00</span>)}
                    </div>
                    <div className="days-grid">
                      {days.map((day) => <div key={day.date} className={`day-column ${day.today ? "is-today" : ""}`} />)}
                      {Array.from({ length: 11 }, (_, i) => <div key={i} className="hour-line" style={{ top: i * 72 }} />)}
                      <div className="now-line" style={{ top: (11.65 - 8) * 72 }}><i /><span>11:39</span></div>
                      {filteredEvents.map((event) => (
                        <button
                          key={event.id}
                          className={`calendar-event ${event.type} ${event.color}`}
                          style={{
                            left: `calc(${event.day} * (100% / 7) + 5px)`,
                            top: (event.start - 8) * 72 + 4,
                            height: event.duration * 72 - 8,
                            width: "calc(100% / 7 - 10px)",
                          }}
                        >
                          <strong>{event.title}</strong><span>{event.meta}</span>
                          {event.type === "study" && <i><Icon name="sparkles" size={10} />자동 배치</i>}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            ) : (
              <div className="month-placeholder">
                <div className="month-grid">
                  {Array.from({ length: 35 }, (_, index) => (
                    <div className={index === 18 ? "current" : ""} key={index}>
                      <span>{index < 5 ? 27 + index : index - 4}</span>
                      {[5, 10, 12, 18, 20, 24, 27].includes(index) && <i />}
                    </div>
                  ))}
                </div>
              </div>
            )}
          </section>

          <section className="bottom-grid">
            <div className="assignment-card">
              <div className="card-heading"><div><h2>진행 중인 과제</h2><p>남은 시간을 확인하고 계획을 이어가세요.</p></div><button>전체 보기 <Icon name="chevron-right" size={14} /></button></div>
              <div className="assignment-list">
                {assignments.map((assignment) => {
                  const percent = Math.round((assignment.done / assignment.total) * 100);
                  return (
                    <button className="assignment-row" key={assignment.title}>
                      <span className={`assignment-icon ${assignment.color}`}><Icon name="book" size={17} /></span>
                      <span className="assignment-info"><strong>{assignment.title}</strong><small><i className={`dot ${assignment.color}`} />{assignment.course} · {assignment.due} 마감</small></span>
                      <span className="progress-info"><span><b>{assignment.done}시간</b> / {assignment.total}시간</span><i><em style={{ width: `${percent}%` }} /></i></span>
                      <span className="percent">{percent}%</span>
                      <Icon name="chevron-right" size={16} />
                    </button>
                  );
                })}
              </div>
            </div>
            <div className="focus-card">
              <div className="focus-top"><span><Icon name="sparkles" /></span><small>다음 학습 블록</small></div>
              <h2>알고리즘 과제</h2>
              <p>오늘 오후 1:00 · 2시간</p>
              <div className="focus-note"><Icon name="clock" size={16} /><span>시작까지 <b>1시간 21분</b> 남았어요</span></div>
              <button onClick={() => setToast(true)}><Icon name="check" size={16} />학습 시작하기</button>
            </div>
          </section>
        </div>
      </main>

      <footer className="status-bar">
        <span><i className="status-ready" />준비</span>
        <span>로컬에 자동 저장됨</span>
        <span>일정 11개 · 과제 3개</span>
        <span>2025-11-19 11:39</span>
      </footer>

      {showForm && (
        <div className="modal-backdrop" onMouseDown={() => setShowForm(false)}>
          <form className="modal" onSubmit={submitAssignment} onMouseDown={(e) => e.stopPropagation()}>
            <div className="modal-heading"><div><span><Icon name="sparkles" /></span><div><h2>새 과제 계획하기</h2><p>마감일부터 역순으로 학습 시간을 자동 배치해 드려요.</p></div></div><button type="button" onClick={() => setShowForm(false)}><Icon name="x" /></button></div>
            <label>과제 제목<input autoFocus value={formTitle} onChange={(e) => setFormTitle(e.target.value)} placeholder="예: 알고리즘 분석 보고서" /></label>
            <label>과목<select><option>알고리즘</option><option>소프트웨어 공학</option><option>데이터베이스</option></select></label>
            <div className="form-row">
              <label>마감일<input type="date" defaultValue="2025-11-28" /></label>
              <label>마감 시간<input type="time" defaultValue="23:59" /></label>
            </div>
            <div className="form-row">
              <label>예상 소요 시간<select defaultValue="8"><option value="4">4시간</option><option value="6">6시간</option><option value="8">8시간</option><option value="10">10시간</option></select></label>
              <label>학습 블록 길이<select defaultValue="2"><option value="1">1시간</option><option value="1.5">1시간 30분</option><option value="2">2시간</option></select></label>
            </div>
            <div className="auto-note"><Icon name="sparkles" size={17} /><p><b>자동 일정 배치</b><span>기존 일정을 피해 가능한 늦은 시간부터 4개의 학습 블록을 배치합니다.</span></p></div>
            <div className="modal-actions"><button type="button" onClick={() => setShowForm(false)}>취소</button><button type="submit"><Icon name="sparkles" size={16} />일정 자동 배치</button></div>
          </form>
        </div>
      )}

      {toast && <div className="toast"><span><Icon name="check" size={16} /></span><div><strong>일정이 준비되었어요</strong><p>빈 시간을 찾아 학습 블록을 배치했습니다.</p></div><button onClick={() => setToast(false)}><Icon name="x" size={15} /></button></div>}
    </div>
  );
}

export default App;
