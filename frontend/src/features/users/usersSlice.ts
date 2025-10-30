import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

interface UsersState {
  searchFilter: string;
}

const initialState: UsersState = {
  searchFilter: '',
};

const usersSlice = createSlice({
  name: 'users',
  initialState,
  reducers: {
    setSearchFilter: (state, action: PayloadAction<string>) => {
      state.searchFilter = action.payload;
    },
    clearSearchFilter: (state) => {
      state.searchFilter = '';
    },
  },
});

export const { setSearchFilter, clearSearchFilter } = usersSlice.actions;
export default usersSlice.reducer;
