import { openAboutSaga } from './help.saga';
import { fetchPreparations } from './preparation.saga';

export const helpSagas = [openAboutSaga];
export const preparationSagas = [fetchPreparations];
