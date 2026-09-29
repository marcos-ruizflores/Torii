/**
 * The board's column grid, shared by the search form and the results board so their
 * columns line up down the page. Tailwind needs literal class names, hence one string
 * per breakpoint with the same template.
 *
 * Columns: date · date · nights · airline · stops · verdict · price · action.
 */
export const BOARD_COLS_MD =
  'md:grid-cols-[6.5rem_6.5rem_4rem_minmax(7rem,1fr)_minmax(6.5rem,8rem)_8rem_7rem_9.75rem]'
export const BOARD_COLS_LG =
  'lg:grid-cols-[6.5rem_6.5rem_4rem_minmax(7rem,1fr)_minmax(6.5rem,8rem)_8rem_7rem_9.75rem]'
